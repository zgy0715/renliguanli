// 智能助手 API 意图回归（Node fetch，UTF-8 中文，绕开 Git Bash GBK 污染）
// 用法：node scripts/verify-assistant-api.mjs
const BASE = 'http://localhost:8080/api'

async function login(username) {
  const r = await fetch(`${BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password: '123456' }),
  })
  return (await r.json()).data.token
}

let token = await login('admin')
const H = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }

async function ask(question) {
  const r = await fetch(`${BASE}/assistant/ask`, { method: 'POST', headers: H, body: JSON.stringify({ question }) })
  const j = await r.json()
  if (j.code !== 0) return { error: `${j.code} ${j.message}` }
  const d = j.data
  return {
    intent: d.intent,
    reply: (d.reply || '').replace(/\n/g, ' ⏎ ').slice(0, 72),
    chart: !!d.chart,
    chartType: d.chart?.type || '-',
    table: !!d.table,
    sug: (d.suggestions || []).length,
  }
}

const cases = [
  ['现在在职多少人', 'OVERVIEW'],
  ['各部门人数', 'DEPT_HEADCOUNT'],
  ['后端研发部多少人', 'DEPT_HEADCOUNT'],
  ['人数最多的部门', 'DEPT_HEADCOUNT'],
  ['平均薪资多少', 'AVG_SALARY'],
  ['招聘漏斗怎样', 'RECRUITMENT'],
  ['BOSS直聘渠道效能', 'RECRUITMENT'],
  ['年龄结构分布', 'STRUCTURE'],
  ['学历分布', 'STRUCTURE'],
  ['人才盘点九宫格', 'NINE_BOX'],
  ['本月入职多少', 'NEW_HIRES'],
  ['离职率趋势怎样', 'RESIGNATIONS'],
  ['部门人效对比', 'DEPT_EFFICIENCY'],
  ['近半年人力成本趋势', 'COST_TREND'],
  ['公司培训情况', 'TRAINING'],
  ['最新绩效分布', 'PERFORMANCE'],
  ['公司能力雷达', 'RADAR'],
  ['组织架构', 'ORG_OVERVIEW'],
  ['帮助', 'HELP'],
  ['阿巴阿巴测试兜底', 'FALLBACK'],
  ['各部门人数', 'DEPT_HEADCOUNT'],  // 用于追问测试（需按序）
  ['那最多的呢', 'DEPT_HEADCOUNT'],  // 多轮续接 → 仍命中部门极值
  ['平均薪资多少', 'AVG_SALARY'],    // 用于 RBAC 前预热
]

let pass = 0
const fail = []
console.log('===== ADMIN 意图回归 =====')
for (let i = 0; i < cases.length; i++) {
  const [q, expect] = cases[i]
  const r = await ask(q)
  const ok = !r.error && r.intent === expect
  if (ok) pass++; else fail.push(`${q} → 期望 ${expect}，实际 ${r.error ? r.error : r.intent}`)
  console.log(`${ok ? 'PASS' : 'FAIL'}  [${r.intent || r.error}] ${q} → ${r.reply || ''}  chart=${r.chartType} table=${r.table} sug=${r.sug}`)
}

// 兜底可达性（修复回归）：任意话题（雷达）后问乱码 → 必须 FALLBACK，而非无条件重放上一话题
await ask('公司能力雷达')
const gibberish = await ask('阿巴阿巴')
const fbOk = gibberish.intent === 'FALLBACK'
if (fbOk) pass++; else fail.push(`乱码「阿巴阿巴」未走 FALLBACK（被重放为 ${gibberish.intent}）`)
console.log(`${fbOk ? 'PASS' : 'FAIL'}  兜底可达 → intent=${gibberish.intent}`)

// 追问续接：先建立「各部门人数」上下文，再问「那最多的呢」→ 应返回部门极值
await ask('各部门人数')
const follow = await ask('那最多的呢')
const followOk = follow.intent === 'DEPT_HEADCOUNT' && (follow.reply || '').includes('最多')
if (followOk) pass++; else fail.push('追问「那最多的呢」未续接部门极值')
console.log(`${followOk ? 'PASS' : 'FAIL'}  追问续接 → ${follow.reply}`)

// 历史接口
const hist = await fetch(`${BASE}/assistant/history`, { headers: H })
const histJson = await hist.json()
const histOk = histJson.code === 0 && Array.isArray(histJson.data) && histJson.data.length >= 2
const hasReplyField = histJson.data.some((m) => m.role === 'assistant' && m.reply)
if (histOk && hasReplyField) pass++; else fail.push('history 未返回带 reply 的对话')
console.log(`${histOk ? 'PASS' : 'FAIL'}  历史接口(${histJson.data?.length} 条，含回复体=${hasReplyField})`)

// ===== EMPLOYEE RBAC =====
console.log('===== EMPLOYEE 角色收敛 =====')
const empToken = await login('employee1')
const EH = { 'Content-Type': 'application/json', Authorization: `Bearer ${empToken}` }
async function askEmp(q) {
  const r = await fetch(`${BASE}/assistant/ask`, { method: 'POST', headers: EH, body: JSON.stringify({ question: q }) })
  return (await r.json()).data
}
const denied = await askEmp('平均薪资多少')
const deniedOk = denied.intent === 'RBAC_DENIED'
if (deniedOk) pass++; else fail.push('EMPLOYEE 问平均薪资未降级')
console.log(`${deniedOk ? 'PASS' : 'FAIL'}  薪资降级 intent=${denied.intent} → ${(denied.reply || '').slice(0, 50)}`)

const costDenied = await askEmp('人力成本趋势')
const costDeniedOk = costDenied.intent === 'RBAC_DENIED'
if (costDeniedOk) pass++; else fail.push('EMPLOYEE 问成本未降级')
console.log(`${costDeniedOk ? 'PASS' : 'FAIL'}  成本降级 intent=${costDenied.intent}`)

const mySal = await askEmp('我的工资')
const mySalOk = (mySal.reply || '').includes('工资条')
if (mySalOk) pass++; else fail.push('EMPLOYEE 我的工资不可答')
console.log(`${mySalOk ? 'PASS' : 'FAIL'}  我的工资 → ${(mySal.reply || '').replace(/\n/g, ' ').slice(0, 60)}`)

const myAtt = await askEmp('我的考勤')
const myAttOk = (myAtt.reply || '').includes('出勤率')
if (myAttOk) pass++; else fail.push('EMPLOYEE 我的考勤不可答')
console.log(`${myAttOk ? 'PASS' : 'FAIL'}  我的考勤 → ${(myAtt.reply || '').replace(/\n/g, ' ').slice(0, 60)}`)

// 组织树权限收敛（修复回归）：EMPLOYEE 不得读取含成员名单的部门树
const treeResp = await fetch(`${BASE}/departments/tree`, { headers: EH })
const treeDeniedOk = treeResp.status === 403
if (treeDeniedOk) pass++; else fail.push(`EMPLOYEE 仍可读部门树（HTTP ${treeResp.status}）`)
console.log(`${treeDeniedOk ? 'PASS' : 'FAIL'}  部门树收敛 → HTTP ${treeResp.status}`)

console.log(`\n===== 汇总：${pass} PASS / ${fail.length} FAIL =====`)
if (fail.length) {
  console.log('失败项:')
  for (const f of fail) console.log('  - ' + f)
  process.exit(1)
}
