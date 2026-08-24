// 无头 Edge + CDP 回归验证前端优化：
//  1) 组织架构树 → 显示部门成员姓名
//  2) 招聘渠道按钮 → 点击 BOSS直聘 等仅显示对应渠道候选人
// （原「驾驶舱部门下拉→雷达」已随智能助手改造移除，能力雷达现由助手「公司能力雷达」意图提供）
import { execFile, execFileSync } from 'node:child_process'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
const PORT = 9224

function sleep(ms) { return new Promise((r) => setTimeout(r, ms)) }

const userData = mkdtempSync(join(tmpdir(), 'hrms-fix-'))
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

// 登录拿 token
let tokenRes
try {
  tokenRes = JSON.parse(execFileSync('curl', ['-s', '-X', 'POST', 'http://localhost:8080/api/auth/login', '-H', 'Content-Type: application/json', '-d', '{"username":"admin","password":"123456"}'], { encoding: 'utf8' })).data.token
} catch { /* ignore */ }
console.log('TOKEN len:', String(tokenRes).length)

await send('Page.navigate', { url: 'http://localhost:5173/login' })
await sleep(1200)
await evaluate(`localStorage.setItem('hrms-token', ${JSON.stringify(tokenRes)}); 'ok'`)

const PASS = []
const FAIL = []
function check(name, ok, detail) {
  const line = `${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? '  → ' + detail : ''}`
  console.log(line)
  ok ? PASS.push(name) : FAIL.push(name)
}

async function apiBodies(urlPart) {
  // 收集匹配 urlPart 的响应体
  const ids = events
    .filter((e) => e.method === 'Network.responseReceived' && e.params.response.url.includes(urlPart))
    .map((e) => e.params.requestId)
  const bodies = []
  for (const id of new Set(ids)) {
    try {
      const r = await send('Network.getResponseBody', { requestId: id })
      if (r.result && r.result.body) bodies.push(r.result.body)
    } catch { /* ignore */ }
  }
  return bodies
}

// ========== 1) 组织架构：成员姓名 ==========
await send('Page.navigate', { url: 'http://localhost:5173/org' })
await sleep(4000)
const orgState = await evaluate(`(() => {
  const members = [...document.querySelectorAll('.tree-member')].map((n) => n.textContent.trim())
  const deptNames = [...document.querySelectorAll('.tree-name')].filter((n) => !n.classList.contains('tree-member')).map((n) => n.textContent.trim())
  return { memberCount: members.length, sample: members.slice(0, 5), depts: deptNames.length }
})()`)
console.log('组织树:', JSON.stringify(orgState))
check('组织树显示成员姓名', (orgState.memberCount ?? 0) >= 20, `${orgState.memberCount} 名成员，样例 ${orgState.sample.join('/')}`)
const treeBodies = await apiBodies('/api/departments/tree')
let treeHasMembers = false
try { treeHasMembers = JSON.parse(treeBodies[treeBodies.length - 1] || '{}').data?.[0]?.members?.length > 0 } catch { /* ignore */ }
check('组织树接口返回 members', treeHasMembers, `${treeBodies.length} 次树请求`)

// ========== 3) 招聘渠道按钮 ==========
await send('Page.navigate', { url: 'http://localhost:5173/recruitment' })
await sleep(4000)
const beforeCh = await evaluate(`(() => {
  const metas = [...document.querySelectorAll('.cand-meta')].map((m) => (m.textContent || '').split('·').pop().trim())
  return { total: document.querySelectorAll('.cand-card').length, channels: [...new Set(metas)] }
})()`)
console.log('全部渠道候选:', JSON.stringify(beforeCh))
check('招聘页加载候选人', (beforeCh.total ?? 0) > 0, `${beforeCh.total} 张卡片，渠道 ${beforeCh.channels.join(',')}`)

// 点击 BOSS直聘 渠道按钮
const chClick = await evaluate(`(async () => {
  const labels = [...document.querySelectorAll('.el-radio-button')]
  const target = labels.find((l) => (l.textContent || '').includes('BOSS直聘'))
  if (!target) return 'no-button'
  target.dispatchEvent(new MouseEvent('click', { bubbles: true }))
  return 'clicked'
})()`)
console.log('渠道按钮:', chClick)
await sleep(2200)

const candReq = events
  .filter((e) => e.method === 'Network.requestWillBeSent' && e.params.request.url.includes('/api/recruitment/candidates'))
  .map((e) => e.params.request.url.replace('http://localhost:5173', ''))
console.log('候选人请求:', JSON.stringify(candReq))
check('渠道按钮发起 channel 过滤请求', candReq.some((u) => u.includes('channel=') && u.includes('BOSS')), candReq.join(' ; '))

const afterCh = await evaluate(`(() => {
  const metas = [...document.querySelectorAll('.cand-meta')].map((m) => (m.textContent || '').split('·').pop().trim())
  return { total: document.querySelectorAll('.cand-card').length, channels: [...new Set(metas)] }
})()`)
console.log('BOSS直聘渠道候选:', JSON.stringify(afterCh))
check('筛选后仅显示 BOSS直聘', (afterCh.total ?? 0) > 0 && afterCh.channels.length === 1 && afterCh.channels[0] === 'BOSS直聘',
  `${afterCh.total} 张卡片，渠道 ${afterCh.channels.join(',')}`)

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
