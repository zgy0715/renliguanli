// 无头 Edge + CDP 冒烟：本次修复涉及的关键页面行为
// 登录失败不跳转 / 工作台待办优先级 / 快捷按钮角色收敛 / 全局搜索 Tab 收敛 /
// 员工页 MANAGER 只读 / 路由角色守卫 / 全局搜索→员工详情
import { execFile, execFileSync } from 'node:child_process'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
const PORT = 9225
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

const userData = mkdtempSync(join(tmpdir(), 'hrms-smoke-'))
const edge = execFile(EDGE, [
  '--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
  `--remote-debugging-port=${PORT}`, `--user-data-dir=${userData}`, 'about:blank',
])
edge.stdout.on('data', () => {})
edge.stderr.on('data', () => {})
await sleep(2500)

let target
for (let i = 0; i < 20; i++) {
  try {
    const list = await (await fetch(`http://localhost:${PORT}/json`)).json()
    target = list.find((t) => t.type === 'page')
    if (target) break
  } catch { /* retry */ }
  await sleep(500)
}
if (!target) { console.error('no target'); process.exit(1) }

const ws = new WebSocket(target.webSocketDebuggerUrl)
await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej })
let seq = 0
const pending = new Map()
const events = []
ws.onmessage = (m) => {
  const msg = JSON.parse(m.data)
  if (msg.id && pending.has(msg.id)) { pending.get(msg.id)(msg); pending.delete(msg.id) }
  else if (msg.method) events.push(msg)
}
function send(method, params = {}) {
  return new Promise((res) => { const id = ++seq; pending.set(id, res); ws.send(JSON.stringify({ id, method, params })) })
}
async function evaluate(expr) {
  const r = await send('Runtime.evaluate', { expression: expr, returnByValue: true, awaitPromise: true })
  if (r.result && r.result.exceptionDetails) return { err: r.result.exceptionDetails.text }
  return r.result?.result?.value
}
await send('Runtime.enable')
await send('Page.enable')

function login(u, p) {
  return JSON.parse(execFileSync('curl', [
    '-s', '-X', 'POST', 'http://localhost:8080/api/auth/login',
    '-H', 'Content-Type: application/json',
    '-d', JSON.stringify({ username: u, password: p }),
  ], { encoding: 'utf8' })).data.token
}

let pass = 0, fail = 0
function check(name, ok, detail) {
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail !== undefined ? '  → ' + detail : ''}`)
  ok ? pass++ : fail++
}

// 1) 登录失败：留在登录页，不整页跳转
await send('Page.navigate', { url: 'http://localhost:5173/login' })
await sleep(1500)
const wrongPwd = await evaluate(`(async () => {
  const inputs = [...document.querySelectorAll('input')]
  const find = (ph) => inputs.find((i) => (i.placeholder || '').includes(ph) || (i.name || '') === ph)
  const u = find('username') || find('账号') || find('用户')
  const p = find('password') || find('密码')
  const setV = (el, v) => { const s = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set; s.call(el, v); el.dispatchEvent(new Event('input', { bubbles: true })) }
  if (u && p) {
    setV(u, 'admin'); setV(p, 'wrong-password')
    const btn = [...document.querySelectorAll('button')].find((b) => (b.textContent || '').includes('登'))
    if (btn) btn.click()
  }
  await new Promise((r) => setTimeout(r, 2000))
  return { path: location.pathname, hasForm: !!document.querySelector('.login-box, .login-card, form') }
})()`)
check('登录失败留在登录页', wrongPwd.path === '/login' && wrongPwd.hasForm, JSON.stringify(wrongPwd))

const setToken = async (tok) => {
  await send('Page.navigate', { url: 'http://localhost:5173/login' })
  await sleep(1200)
  await evaluate(`localStorage.setItem('hrms-token', ${JSON.stringify(tok)}); 'ok'`)
}

// 2) hr 工作台：待办优先级 tag（中文高/中/低）+ 快捷按钮 4 个（admin 无待办，待办按角色分配）
const adminTok = login('admin', '123456')
const hrTok = login('hr', '123456')
await setToken(hrTok)
await send('Page.navigate', { url: 'http://localhost:5173/dashboard' })
await sleep(4000)
const wb = await evaluate(`(() => ({
  todoTags: [...document.querySelectorAll('.todo-item .el-tag')].map((t) => (t.textContent || '').trim()),
  todoDotClass: [...document.querySelectorAll('.todo-item')].slice(0, 5).map((d) => d.className),
  quickBtns: [...document.querySelectorAll('.welcome-right button')].map((b) => (b.textContent || '').trim()),
  payrollWidget: [...document.querySelectorAll('.widget-title')].map((w) => (w.textContent || '').trim()).some((t) => t.includes('薪酬成本')),
}))()`)
check('工作台待办优先级 tag', wb.todoTags.length > 0 && wb.todoTags.every((t) => ['高', '中', '低'].includes(t)), wb.todoTags.join(',') || 'none')
check('工作台待办圆点色类', wb.todoDotClass.some((cl) => /todo-item--(high|medium|low)/.test(cl)), wb.todoDotClass.join(' | '))
check('工作台快捷按钮(hr=4)', wb.quickBtns.length === 4, wb.quickBtns.join(','))

// 3) employee 工作台：快捷按钮只留智能助手，且无薪酬组件
const empTok = login('employee1', '123456')
await setToken(empTok)
await send('Page.navigate', { url: 'http://localhost:5173/dashboard' })
await sleep(4000)
const wbE = await evaluate(`(() => ({
  quickBtns: [...document.querySelectorAll('.welcome-right button')].map((b) => (b.textContent || '').trim()),
  payrollWidget: [...document.querySelectorAll('.widget-title')].map((w) => (w.textContent || '').trim()).some((t) => t.includes('薪酬成本')),
}))()`)
check('员工工作台快捷按钮(仅智能助手)', wbE.quickBtns.length === 1 && wbE.quickBtns[0] === '智能助手', wbE.quickBtns.join(',') || 'none')
check('员工工作台无薪酬组件', wbE.payrollWidget === false, String(wbE.payrollWidget))

// 4) employee 全局搜索：仅「功能」Tab，无「员工」
await evaluate(`document.querySelector('.global-search').dispatchEvent(new MouseEvent('click', { bubbles: true })); 'ok'`)
await sleep(800)
const gsE = await evaluate(`[...document.querySelectorAll('.search-tab')].map((t) => t.textContent.trim())`)
check('员工全局搜索仅功能Tab', gsE.length === 1 && gsE[0] === '功能', gsE.join(','))

// 5) manager 员工页：无新增/编辑/删除按钮，有详情
const mgrTok = login('manager1', '123456')
await setToken(mgrTok)
await send('Page.navigate', { url: 'http://localhost:5173/personnel/employees' })
await sleep(4000)
const mgrPage = await evaluate(`(() => {
  const row = document.querySelector('.el-table__row')
  return {
    hasAdd: [...document.querySelectorAll('button')].some((b) => (b.textContent || '').includes('新增员工')),
    opsCol: row ? [...row.querySelectorAll('button')].map((b) => (b.textContent || '').trim()) : [],
  }
})()`)
check('员工页无新增按钮(MANAGER)', mgrPage.hasAdd === false, String(mgrPage.hasAdd))
check('员工页操作列仅详情', mgrPage.opsCol.filter((t) => ['详情', '编辑', '删除'].includes(t)).join(',') === '详情', mgrPage.opsCol.join(','))

// 6) admin 员工页：有新增 + 编辑/删除
await setToken(adminTok)
await send('Page.navigate', { url: 'http://localhost:5173/personnel/employees' })
await sleep(4000)
const admPage = await evaluate(`(() => {
  const row = document.querySelector('.el-table__row')
  return {
    hasAdd: [...document.querySelectorAll('button')].some((b) => (b.textContent || '').includes('新增员工')),
    opsCol: row ? [...row.querySelectorAll('button')].map((b) => (b.textContent || '').trim()) : [],
  }
})()`)
check('员工页有新增按钮(ADMIN)', admPage.hasAdd === true, String(admPage.hasAdd))
check('员工页操作列含编辑删除(ADMIN)', ['详情', '编辑', '删除'].every((x) => admPage.opsCol.includes(x)), admPage.opsCol.join(','))

// 7) 路由守卫：EMPLOYEE 直达 /system 被弹回 /dashboard
await setToken(empTok)
await send('Page.navigate', { url: 'http://localhost:5173/system' })
await sleep(2500)
const guard = await evaluate(`location.pathname`)
check('路由守卫拦截 EMPLOYEE 访问/system', guard === '/dashboard', guard)

// 8) 全局搜索员工跳转 → 员工详情抽屉打开
await setToken(adminTok)
await send('Page.navigate', { url: 'http://localhost:5173/dashboard' })
await sleep(3500)
await evaluate(`document.querySelector('.global-search').dispatchEvent(new MouseEvent('click', { bubbles: true })); 'ok'`)
await sleep(600)
const gsDiag = await evaluate(`(async () => {
  const i = document.querySelector('.global-search-panel input')
  if (!i) return { err: 'no-search-input' }
  const s = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set
  s.call(i, '赵旭'); i.dispatchEvent(new Event('input', { bubbles: true }))
  await new Promise((r) => setTimeout(r, 2000))
  const items = [...document.querySelectorAll('.result-item')].map((x) => (x.textContent || '').replace(/\\s+/g, ' ').trim())
  const item = [...document.querySelectorAll('.result-item')].find((x) => (x.textContent || '').includes('赵旭'))
  if (item) item.click()
  await new Promise((r) => setTimeout(r, 500))
  return { items: items.slice(0, 4), clicked: !!item, path: location.pathname }
})()`)
console.log('全局搜索诊断:', JSON.stringify(gsDiag))
await sleep(3500)
const detailOpen = await evaluate(`(document.querySelector('.el-drawer .detail-name') ? (document.querySelector('.el-drawer .detail-name').textContent || '').trim() : 'none')`)
check('全局搜索→员工详情抽屉', typeof detailOpen === 'string' && detailOpen.includes('赵旭'), String(detailOpen))

// 汇总
const exceptions = events.filter((e) => e.method === 'Runtime.exceptionThrown')
for (const e of exceptions) {
  const ex = e.params.exceptionDetails
  console.log('EXCEPTION:', ex.text, (ex.exception?.description || '').split('\n')[0])
}
check('无前端异常', exceptions.length === 0, `${exceptions.length} 个异常`)

console.log(`===== 汇总：${pass} PASS / ${fail} FAIL =====`)
ws.close()
edge.kill()
try { rmSync(userData, { recursive: true, force: true }) } catch { /* ignore */ }
process.exit(fail ? 1 : 0)
