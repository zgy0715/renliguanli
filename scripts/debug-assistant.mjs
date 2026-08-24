// 无头 Edge + CDP 回归验证「智能助手」页：
//  1) 欢迎页 + 起始问题渲染
//  2) 提问 → 助手气泡（自然语言结论）出现
//  3) 图表 / 表格渲染（canvas / table）
//  4) 推荐追问点击 → 新一轮问答
//  5) 多轮追问「那最多的呢」→ 续接上一话题
//  6) 刷新后历史还原
//  7) EMPLOYEE 角色薪资意图降级
import { execFile, execFileSync } from 'node:child_process'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
const PORT = 9225

function sleep(ms) { return new Promise((r) => setTimeout(r, ms)) }

const userData = mkdtempSync(join(tmpdir(), 'hrms-ai-'))
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
  if (r.result && r.result.exceptionDetails) return { error: r.result.exceptionDetails.text }
  return r.result?.result?.value
}

await send('Runtime.enable')
await send('Page.enable')
await send('Network.enable')
await send('Log.enable')

function login(username) {
  try {
    const raw = execFileSync('curl', ['-s', '-X', 'POST', 'http://localhost:8080/api/auth/login', '-H', 'Content-Type: application/json', '-d', JSON.stringify({ username, password: '123456' })], { encoding: 'utf8' })
    return JSON.parse(raw).data.token
  } catch { return '' }
}

const PASS = []
const FAIL = []
function check(name, ok, detail) {
  const line = `${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? '  → ' + detail : ''}`
  console.log(line)
  ok ? PASS.push(name) : FAIL.push(name)
}

async function gotoAssistant(token) {
  await send('Page.navigate', { url: 'http://localhost:5173/login' })
  await sleep(1000)
  await evaluate(`localStorage.setItem('hrms-token', ${JSON.stringify(token)}); 'ok'`)
  await send('Page.navigate', { url: 'http://localhost:5173/assistant' })
  await sleep(1800)
}

async function ask(q) {
  const r = await evaluate(`(async () => {
    const input = document.querySelector('.assistant-input .el-input__inner')
    if (!input) return 'no-input'
    const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set
    setter.call(input, ${JSON.stringify(q)})
    input.dispatchEvent(new Event('input', { bubbles: true }))
    await new Promise(r => setTimeout(r, 300))
    const before = document.querySelectorAll('.bubble.assistant').length
    document.querySelector('.assistant-input .el-button--primary').dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await new Promise(r => setTimeout(r, 2000))
    const bubbles = [...document.querySelectorAll('.bubble.assistant .bubble-text')]
    const last = bubbles.length ? bubbles[bubbles.length - 1].textContent.trim() : ''
    const charts = document.querySelectorAll('.bubble-chart canvas').length
    const tables = document.querySelectorAll('.bubble-table table').length
    return { added: bubbles.length - before, last: last.slice(0, 60), charts, tables }
  })()`)
  return r
}

// ========== 1) ADMIN：欢迎页 + 多意图问答 ==========
const adminToken = login('admin')
check('登录获取 admin token', adminToken.length > 50, `len=${adminToken.length}`)
// 清空历史，保证欢迎页可见（历史已可能从 API 回归中累积）
try {
  execFileSync('curl', ['-s', '-X', 'DELETE', 'http://localhost:8080/api/assistant/history', '-H', `Authorization: Bearer ${adminToken}`], { encoding: 'utf8' })
} catch { /* ignore */ }
await gotoAssistant(adminToken)

const welcome = await evaluate(`(() => ({
  hasWelcome: !!document.querySelector('.assistant-welcome'),
  starters: document.querySelectorAll('.starter-chip').length,
  title: (document.querySelector('.assistant-name') || {}).textContent || '',
}))()`)
console.log('欢迎页:', JSON.stringify(welcome))
check('助手欢迎页渲染', welcome.hasWelcome && welcome.starters === 6, `starters=${welcome.starters}`)

// 问「各部门人数？」→ 柱状图
const r1 = await ask('各部门人数？')
console.log('Q1 部门人数:', JSON.stringify(r1))
check('Q1 生成助手气泡', (r1.added ?? 0) >= 1 && (r1.last || '').includes('部门'), r1.last)
check('Q1 渲染柱状图 canvas', (r1.charts ?? 0) >= 1, `charts=${r1.charts}`)

// 问「平均薪资多少？」→ 折线图
const r2 = await ask('平均薪资多少？')
console.log('Q2 平均薪资:', JSON.stringify(r2))
check('Q2 平均薪资回答', (r2.last || '').includes('平均月薪'), r2.last)

// 推荐追问 chip 点击 → 新一轮
const chipClick = await evaluate(`(async () => {
  const chips = [...document.querySelectorAll('.suggestion-chip')]
  if (!chips.length) return 'no-chip'
  chips[0].dispatchEvent(new MouseEvent('click', { bubbles: true }))
  await new Promise(r => setTimeout(r, 2000))
  return document.querySelectorAll('.bubble.assistant').length
})()`)
check('点击推荐追问产生新一轮', typeof chipClick === 'number' && chipClick >= 3, `气泡总数=${chipClick}`)

// 多轮追问续接：「各部门人数？」→「那最多的呢」
await ask('各部门人数？')
const r3 = await ask('那最多的呢')
console.log('Q3 追问:', JSON.stringify(r3))
check('追问续接上一话题', (r3.last || '').includes('最多') || (r3.last || '').includes('部门'), r3.last)

// 表格（人才盘点九宫格）
const r4 = await ask('人才盘点九宫格')
console.log('Q4 九宫格:', JSON.stringify(r4))
check('九宫格返回表格', (r4.tables ?? 0) >= 1, `tables=${r4.tables}`)

// 刷新后历史还原
await send('Page.navigate', { url: 'http://localhost:5173/assistant' })
await sleep(1800)
const restored = await evaluate(`document.querySelectorAll('.bubble.assistant').length`)
check('刷新后历史还原', restored >= 4, `还原 ${restored} 条助手气泡`)
// 还原后的图表也应恢复
const restoredCharts = await evaluate(`document.querySelectorAll('.bubble-chart canvas').length`)
check('历史还原含图表', restoredCharts >= 1, `还原图表 ${restoredCharts}`)

// ========== 2) EMPLOYEE：薪资降级 ==========
const empToken = login('employee1')
check('登录获取 employee token', empToken.length > 50, `len=${empToken.length}`)
await gotoAssistant(empToken)

const re = await ask('平均薪资多少？')
console.log('E1 员工问薪资:', JSON.stringify(re))
check('EMPLOYEE 薪资降级', /HR\s*\/\s*管理者/.test(re.last || '') || (re.last || '').includes('仅对'), re.last)

const rp = await ask('我的工资')
console.log('E2 我的工资:', JSON.stringify(rp))
check('个人工资条可答', (rp.last || '').includes('工资条'), rp.last)

const rk = await ask('我的考勤')
console.log('E3 我的考勤:', JSON.stringify(rk))
check('个人考勤可答', (rk.last || '').includes('出勤率'), rk.last)

// ========== 汇总 ==========
const exceptions = events.filter((e) => e.method === 'Runtime.exceptionThrown')
for (const e of exceptions) {
  const ex = e.params.exceptionDetails
  console.log('EXCEPTION:', ex.text, (ex.exception?.description || '').split('\n')[0])
}
check('无前端异常', exceptions.length === 0, `${exceptions.length} 个异常`)

console.log(`\n===== 汇总：${PASS.length} PASS / ${FAIL.length} FAIL =====`)
console.log(FAIL.length ? '失败项:\n' + FAIL.map((f) => '  - ' + f).join('\n') : '全部通过 ✔')

ws.close()
edge.kill()
try { rmSync(userData, { recursive: true, force: true }) } catch { /* Edge 可能未完全释放 */ }
process.exit(FAIL.length ? 1 : 0)
