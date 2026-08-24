# -*- coding: utf-8 -*-
"""
HRMS 答辩汇报 PPT 生成器 v3 — 依据 docs/06-答辩汇报PPT大纲.md（内容规格 v3）
输出：docs/HRMS答辩汇报.pptx
设计要点：严格网格、图片等比 contain 适配、每页少量短句、统一页眉页脚。
"""
import os
import struct
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from pptx.oxml.ns import qn

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SHOT = os.path.join(BASE, "docs", "截图")
OUT = os.path.join(BASE, "docs", "HRMS答辩汇报.pptx")

# ---------- 色板 ----------
BLUE = RGBColor(0x2F, 0x6B, 0xF0)
NAVY = RGBColor(0x1F, 0x2A, 0x44)
AMBER = RGBColor(0xF0, 0xB4, 0x00)
GRAY = RGBColor(0x55, 0x5F, 0x71)
LIGHT = RGBColor(0xF2, 0xF6, 0xFE)
BORDER = RGBColor(0xD9, 0xE2, 0xF6)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
SKY = RGBColor(0xC9, 0xD5, 0xF5)
RED = RGBColor(0xC0, 0x39, 0x2B)
GREEN = RGBColor(0x1E, 0x84, 0x44)
FONT = "微软雅黑"

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
SW, SH = prs.slide_width, prs.slide_height
BLANK = prs.slide_layouts[6]

# ---------- 网格常量 ----------
MX = 0.7                 # 左右页边距(in)
CW = 13.333 - MX * 2     # 内容宽
TOP = 1.75               # 内容区顶
BOT = 6.95               # 内容区底


def F(run, size, color, bold=False):
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = color
    run.font.name = FONT
    rPr = run._r.get_or_add_rPr()
    ea = rPr.find(qn('a:ea'))
    if ea is None:
        ea = rPr.makeelement(qn('a:ea'), {})
        rPr.append(ea)
    ea.set('typeface', FONT)


def rect(s, x, y, w, h, fill, shape=MSO_SHAPE.RECTANGLE, line=None, radius=None):
    sp = s.shapes.add_shape(shape, Inches(x), Inches(y), Inches(w), Inches(h))
    sp.fill.solid()
    sp.fill.fore_color.rgb = fill
    if line is None:
        sp.line.fill.background()
    else:
        sp.line.color.rgb = line
        sp.line.width = Pt(1)
    sp.shadow.inherit = False
    if radius is not None and shape == MSO_SHAPE.ROUNDED_RECTANGLE:
        try:
            sp.adjustments[0] = radius
        except Exception:
            pass
    return sp


def tb(s, x, y, w, h, lines, size=14, color=GRAY, bold=False,
       align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP, gap=0, lh=None):
    """多行文本；lines 为 str（\n 分隔）或 list；gap=段后(pt)。"""
    if isinstance(lines, str):
        lines = lines.split("\n")
    box = s.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = box.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = anchor
    for i, ln in enumerate(lines):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        if gap:
            p.space_after = Pt(gap)
        if lh:
            p.line_spacing = lh
        r = p.add_run()
        r.text = ln
        F(r, size, color, bold)
    return box


def png_size(path):
    with open(path, 'rb') as f:
        d = f.read(24)
    w, h = struct.unpack('>II', d[16:24])
    return w, h


def fit_pic(s, path, x, y, w, h, frame=True):
    """图片等比 contain 于 (x,y,w,h) 框内，居中；可选浅框。返回是否成功。"""
    if frame:
        rect(s, x, y, w, h, WHITE, line=BORDER)
    if not os.path.exists(path):
        return False
    iw, ih = png_size(path)
    ir = iw / ih
    fr = w / h
    if ir > fr:
        nw, nh = w, w / ir
    else:
        nh, nw = h, h * ir
    nx = x + (w - nw) / 2
    ny = y + (h - nh) / 2
    s.shapes.add_picture(path, Inches(nx), Inches(ny), width=Inches(nw), height=Inches(nh))
    return True


def header(s, eyebrow, title, subtitle=""):
    rect(s, 0, 0, 13.333, 0.12, BLUE)  # 顶部色条
    tb(s, MX, 0.34, CW, 0.3, eyebrow, size=13, color=BLUE, bold=True)
    tb(s, MX, 0.62, CW, 0.62, title, size=28, color=NAVY, bold=True)
    if subtitle:
        tb(s, MX, 1.24, CW, 0.34, subtitle, size=13, color=GRAY)
    rect(s, MX, 1.62, 0.55, 0.045, AMBER)


def footer(s, no):
    rect(s, MX, 7.08, CW, 0.012, BORDER)
    tb(s, MX, 7.14, 8, 0.26, "企业员工管理系统 HRMS · 答辩汇报", size=9, color=GRAY)
    tb(s, 12.0, 7.12, 0.63, 0.3, f"{no:02d}", size=12, color=BLUE, bold=True, align=PP_ALIGN.RIGHT)


def page(eyebrow, title, subtitle=""):
    s = prs.slides.add_slide(BLANK)
    header(s, eyebrow, title, subtitle)
    return s


def bullets(s, items, x, y, w, size=15, gap=12, color=GRAY, marker="•"):
    box = s.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(BOT - y))
    tf = box.text_frame
    tf.word_wrap = True
    for i, it in enumerate(items):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.space_after = Pt(gap)
        r = p.add_run()
        r.text = f"{marker}  {it}" if marker else it
        F(r, size, color, False)
    return box


def card(s, x, y, w, h, title, body, tcolor=BLUE, size=13.5, tsize=16):
    rect(s, x, y, w, h, LIGHT, MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.055)
    rect(s, x, y, 0.09, h, tcolor)
    tb(s, x + 0.32, y + 0.16, w - 0.55, 0.4, title, size=tsize, color=tcolor, bold=True)
    tb(s, x + 0.32, y + 0.62, w - 0.55, h - 0.75, body, size=size, gap=7)


def table(s, x, y, w, h, data, widths=None, size=14):
    rows, cols = len(data), len(data[0])
    gf = s.shapes.add_table(rows, cols, Inches(x), Inches(y), Inches(w), Inches(h))
    t = gf.table
    if widths:
        tot = sum(widths)
        for i, cw in enumerate(widths):
            t.columns[i].width = Emu(int(Inches(w) * cw / tot))
    for ri, row in enumerate(data):
        t.rows[ri].height = Emu(int(Inches(h) / rows))
        for ci, v in enumerate(row):
            c = t.cell(ri, ci)
            c.margin_top = c.margin_bottom = Inches(0.02)
            c.margin_left = c.margin_right = Inches(0.06)
            c.vertical_anchor = MSO_ANCHOR.MIDDLE
            c.fill.solid()
            if ri == 0:
                c.fill.fore_color.rgb = NAVY
                col, bold = WHITE, True
            else:
                c.fill.fore_color.rgb = WHITE if ri % 2 else LIGHT
                col, bold = GRAY, False
                if ci == 0:
                    col, bold = NAVY, True
            tf = c.text_frame
            tf.word_wrap = True
            p = tf.paragraphs[0]
            p.alignment = PP_ALIGN.LEFT if ci == 0 else PP_ALIGN.CENTER
            r = p.add_run()
            r.text = str(v)
            F(r, size, col, bold)
    return gf


# ================= P1 封面 =================
s = prs.slides.add_slide(BLANK)
rect(s, 0, 0, 13.333, 7.5, NAVY)
rect(s, 0, 0, 13.333, 0.14, BLUE)
rect(s, 0, 7.36, 13.333, 0.14, BLUE)
tb(s, 1.0, 1.55, 11.3, 0.4, "全栈项目 · AI 工程化实践", size=15, color=AMBER, bold=True)
tb(s, 1.0, 2.15, 11.3, 1.2, "企业员工管理系统", size=54, color=WHITE, bold=True)
tb(s, 1.0, 3.35, 11.3, 0.7, "HRMS · Human Resource Management System", size=20, color=SKY)
rect(s, 1.05, 4.25, 3.2, 0.05, AMBER)
tb(s, 1.0, 4.55, 11.3, 0.5, "以 AI 为引擎 · 以数据为中心的人力资源数字化平台", size=19, color=SKY)
tb(s, 1.0, 5.9, 11.3, 0.9,
   "答辩人：赵广雨（2607-3 · 第十七组）     指导老师：____________     2026 年",
   size=14, color=RGBColor(0x9F, 0xB0, 0xD8))

# ================= P2 团队介绍与任务分工 =================
s = page("TEAM", "团队介绍与任务分工", "2607-3 单人组 · 第十七组")
card(s, MX, TOP, 5.55, 4.55, "成员介绍",
     "赵广雨\n\n2607-3 单人组 · 第十七组\n\n一人全流程：产品 · 全栈 · 测试 · 文档\n\nAI 结对开发，小步提交持续交付", size=14.5)
team = [
    ["任务域", "工作内容", "交付产出"],
    ["需求与设计", "产品架构 · 数据库 · API 设计", "6 份设计文档"],
    ["后端开发", "22 实体 · 15 控制器 · 安全体系", "Spring Boot 服务"],
    ["前端开发", "12 页面 · 10+ 图表 · 权限路由", "Vue3 SPA"],
    ["测试验证", "51 用例 · 多代理并行回归", "测试报告"],
    ["部署交付", "CloudStudio 一键启动", "公网可访问"],
]
table(s, 6.6, TOP, 6.03, 3.55, team, widths=[1.8, 3.3, 2.2], size=12)
rect(s, MX, 6.35, CW, 0.62, NAVY, MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.5)
tb(s, MX, 6.35, CW, 0.62, "单人组全流程独立交付 · 46 次 Git 提交全程留痕",
   size=15, color=WHITE, bold=True, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
footer(s, 2)

# ================= P3 目录 =================
s = page("CONTENTS", "目录", "九章脉络 · 九分钟讲完")
toc = [
    ("01", "背景与选题", "痛点与解法"),
    ("02", "需求与设计", "产品总览 · RBAC"),
    ("03", "技术实现", "架构 · 数据库 · API · 安全"),
    ("04", "核心功能", "AI 助手 · 驾驶舱 · 考勤薪酬"),
    ("05", "质量与测试", "用例 · 工程实践"),
    ("06", "总结与展望", "成果 · 创新 · 下一步"),
]
for i, (num, t, sub) in enumerate(toc):
    col = i // 3
    row = i % 3
    x = MX + col * 6.35
    y = 2.0 + row * 1.62
    tb(s, x, y, 1.0, 0.75, num, size=34, color=BLUE, bold=True)
    tb(s, x + 1.05, y + 0.04, 4.9, 0.45, t, size=19, color=NAVY, bold=True)
    tb(s, x + 1.05, y + 0.52, 4.9, 0.35, sub, size=12, color=GRAY)
    rect(s, x, y + 1.12, 5.6, 0.012, BORDER)
footer(s, 3)

# ================= P3 背景与痛点 =================
s = page("01 · 背景与选题", "背景与痛点 → 选题", "先讲问题，再亮答案")
card(s, MX, TOP, 5.55, 4.35, "现状痛点",
     "事务割裂：多系统 / Excel 并行\n数据滞后：报表靠人工汇总\n决策无据：缺少实时看板\n协作低效：流程靠口头传递",
     tcolor=RED)
card(s, 7.05, TOP, 5.55, 4.35, "本系统解法",
     "一套平台管完「选 · 用 · 育 · 留」\nAI 问数：自然语言一问即得\n驾驶舱：9 维数据辅助决策\n考勤薪酬绩效全流程线上闭环",
     tcolor=GREEN)
ar = rect(s, 6.32, 3.55, 0.62, 0.55, BLUE, MSO_SHAPE.RIGHT_ARROW)
rect(s, MX, 6.35, CW, 0.62, NAVY, MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.5)
tb(s, MX, 6.35, CW, 0.62, "让人事事务「一套管完」 · 让经营数据「一问即得」",
   size=17, color=WHITE, bold=True, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
footer(s, 4)

# ================= P4 产品总览 =================
s = page("02 · 需求与设计", "产品总览 · 九大模块", "一套系统覆盖 HR 全业务")
mods = [
    ("组织人事", "部门树 · 岗位 · 编制"),
    ("员工档案", "全生命周期 + 标签"),
    ("考勤排班", "月历 · 异常 · 归档"),
    ("薪酬核算", "幂等算薪 · 状态机"),
    ("绩效管理", "目标 · 人才九宫格"),
    ("培训发展", "课程 · 学习记录"),
    ("招聘管理", "职位 · 招聘漏斗"),
    ("AI 智能助手", "自然语言问数 ★"),
    ("数据驾驶舱", "9 维分析看板"),
]
cw_, ch_, gx, gy = 3.9, 1.52, 0.14, 0.14
for i, (t, d) in enumerate(mods):
    x = MX + (i % 3) * (cw_ + gx)
    y = TOP + (i // 3) * (ch_ + gy)
    hot = i >= 7
    rect(s, x, y, cw_, ch_, LIGHT if not hot else RGBColor(0xE8, 0xF0, 0xFE),
         MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.08)
    rect(s, x, y, cw_, 0.09, BLUE if not hot else AMBER)
    tb(s, x + 0.28, y + 0.24, cw_ - 0.5, 0.42, t, size=17,
       color=NAVY if not hot else RGBColor(0xB4, 0x78, 0x00), bold=True)
    tb(s, x + 0.28, y + 0.72, cw_ - 0.5, 0.5, d, size=12, color=GRAY)
footer(s, 5)

# ================= P5 RBAC =================
s = page("02 · 需求与设计", "角色与权限（RBAC）", "菜单 · 接口 · 数据 三维度收敛")
data = [
    ["能力域", "管理员", "HR 专员", "部门主管", "普通员工"],
    ["工作台 · 数据分析", "✓", "✓", "✓", "受限"],
    ["组织人事 · 档案", "✓", "✓", "✓", "仅本人"],
    ["考勤排班", "✓", "✓", "✓", "仅本人"],
    ["薪酬核算", "✓", "✓", "—", "工资条"],
    ["绩效 · 培训 · 招聘", "✓", "✓", "✓", "—"],
    ["系统管理", "✓", "—", "—", "—"],
    ["员工自助", "✓", "✓", "✓", "✓"],
]
table(s, MX, TOP, CW, 4.3, data, widths=[3.4, 1.7, 1.7, 1.8, 1.9], size=14)
rect(s, MX, 6.35, CW, 0.62, LIGHT, MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.5)
tb(s, MX, 6.35, CW, 0.62,
   "前端路由收敛菜单 + 后端 @PreAuthorize 校验双重拦截，越权访问一律 403",
   size=15, color=BLUE, bold=True, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
footer(s, 6)

# ================= P6 架构 =================
s = page("03 · 技术实现", "系统架构", "前后端分离 · 四层职责")
layers = [
    ("展示层", "Vue3 · TypeScript · Element Plus · ECharts · Pinia", BLUE),
    ("接入层", "路由守卫（角色收敛）· Axios（JWT 注入）", RGBColor(0x4C, 0x7F, 0xF2)),
    ("业务层", "Spring Boot 3 · Controller / Service / Repository · AOP 审计", RGBColor(0x69, 0x93, 0xF5)),
    ("数据层", "Spring Data JPA · H2 演示 / MySQL 可选 · 固定种子", RGBColor(0x86, 0xA7, 0xF8)),
]
yy = TOP
for name, desc, c in layers:
    rect(s, MX, yy, CW, 1.02, LIGHT, MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.09)
    rect(s, MX, yy, 2.1, 1.02, c)
    tb(s, MX, yy, 2.1, 1.02, name, size=18, color=WHITE, bold=True,
       align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    tb(s, MX + 2.45, yy, CW - 2.75, 1.02, desc, size=15, color=GRAY,
       anchor=MSO_ANCHOR.MIDDLE)
    yy += 1.19
rect(s, MX, yy + 0.06, CW, 0.55, NAVY, MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.5)
tb(s, MX, yy + 0.06, CW, 0.55,
   "跨层能力：JWT(HS384·24h) · BCrypt · 统一响应与异常 · 操作留痕",
   size=14, color=WHITE, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
footer(s, 7)

# ================= P7 数据库 =================
s = page("03 · 技术实现", "数据库设计", "22 张表 · 7 大业务域 · 固定种子可复现")
card(s, MX, TOP, 6.1, 4.55, "核心 ER 关系",
     "department · 部门树（parent_id 自关联）\n　↓\nemployee · 员工主档（部门 / 岗位 / 上级）\n　↓\nattendance_record · payroll_record\nperformance_record · train_record\nrecruitment_candidate · hr_todo …", size=12.5)
stats = [
    ("22", "张核心表 · 7 业务域"),
    ("61", "名员工 · 22 部门 · 46 岗位"),
    ("7,636", "条考勤记录"),
    ("399", "条薪酬记录 · 固定种子复现"),
]
for i, (num, desc) in enumerate(stats):
    x = 7.15 + (i % 2) * 2.85
    y = TOP + (i // 2) * 2.35
    rect(s, x, y, 2.7, 2.2, LIGHT, MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.07)
    tb(s, x, y + 0.38, 2.7, 0.75, num, size=32, color=BLUE, bold=True, align=PP_ALIGN.CENTER)
    tb(s, x + 0.2, y + 1.3, 2.3, 0.75, desc, size=11.5, color=GRAY, align=PP_ALIGN.CENTER)
footer(s, 8)

# ================= P8 API 与安全 =================
s = page("03 · 技术实现", "API 规范与安全体系", "接口统一 · 安全内建")
card(s, MX, TOP, 5.55, 4.55, "API 设计",
     "统一响应：{ code, message, data }\n\ncode=0 成功\n401 未认证 · 403 越权\n400 参数错误 · 404 不存在\n\n15 个 Controller · RESTful 风格", size=14)
card(s, 7.05, TOP, 5.55, 4.55, "安全体系",
     "认证：JWT（HS384 · 24h 有效期）\n\n存储：BCrypt 哈希 · 接口脱敏\n\n鉴权：@PreAuthorize 接口级校验\n\n防护：智能助手 XSS 实体转义\n\n审计：AOP 关键操作留痕", size=14)
footer(s, 9)

# ================= P9 AI 助手 =================
s = page("04 · 核心功能", "AI 智能助手 ★", "自然语言 → 结论 · 图表 · 追问")
bullets(s, [
    "一句话问数：「各部门有多少员工」",
    "返回：结论 + 柱状图 + 可追问",
    "规则式 NLP：18 类意图，结果可测",
    "多轮对话：「那最多的呢？」自动续接",
    "角色收敛：员工问薪酬 → 降级提示",
    "零外部依赖：离线可用，响应毫秒级",
], MX, TOP + 0.15, 6.6, size=16, gap=16)
fit_pic(s, os.path.join(SHOT, "7-智能助手.png"), 7.6, TOP, 5.03, 4.75)
footer(s, 10)

# ================= P10 驾驶舱/组织/档案 =================
s = page("04 · 核心功能", "数据驾驶舱 · 组织架构 · 员工档案", "三屏速览")
trio = [("1-工作台.png", "数据驾驶舱"), ("3-组织架构.png", "组织架构树"), ("员工档案.png", "员工档案")]
iw = 3.9
for i, (fn, cap) in enumerate(trio):
    x = MX + i * (iw + 0.14)
    fit_pic(s, os.path.join(SHOT, fn), x, TOP, iw, 3.35)
    tb(s, x, TOP + 3.42, iw, 0.34, cap, size=13.5, color=BLUE, bold=True, align=PP_ALIGN.CENTER)
bullets(s, [
    "9 维分析：九宫格 · 招聘漏斗 · 趋势 · 成本",
    "组织树：节点人数自动汇总，成员可下钻",
    "档案：全生命周期 + 标签 + 完整度评分",
], MX, 5.75, CW, size=14.5, gap=8)
footer(s, 11)

# ================= P11 考勤/薪酬 =================
s = page("04 · 核心功能", "考勤排班 · 薪酬核算", "管理闭环的两个硬模块")
duo = [("5-考勤排班.png", "考勤排班：月历热力 · 异常识别 · 一键归档"),
       ("6-薪酬核算.png", "薪酬核算：幂等算薪 · 社保个税 · 成本构成")]
iw = 5.95
for i, (fn, cap) in enumerate(duo):
    x = MX + i * (iw + 0.15)
    fit_pic(s, os.path.join(SHOT, fn), x, TOP, iw, 3.35)
    tb(s, x, TOP + 3.42, iw, 0.34, cap, size=13.5, color=BLUE, bold=True, align=PP_ALIGN.CENTER)
rect(s, MX, 5.75, CW, 0.9, LIGHT, MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.12)
tb(s, MX + 0.35, 5.75, CW - 0.7, 0.9,
   "薪酬状态机：DRAFT → CALCULATED → CONFIRMED → PAID　·　核算幂等可重跑　·　KPI 实时联动",
   size=15, color=NAVY, bold=True, anchor=MSO_ANCHOR.MIDDLE)
footer(s, 12)

# ================= P12 工程实践 =================
s = page("05 · 质量与实践", "AI-Coding 工程实践", "会工程化交付，而不只是会写代码")
steps = ["提示词\n驱动开发", "设计文档\n先行", "设计审查\nReview", "编码\n与测试", "全程\n留痕"]
n = len(steps)
chev_w, ov = 2.62, 0.34
total = n * chev_w - (n - 1) * ov
x0 = MX + (CW - total) / 2
for i, st in enumerate(steps):
    x = x0 + i * (chev_w - ov)
    c = BLUE if i % 2 == 0 else RGBColor(0x4C, 0x7F, 0xF2)
    sp = rect(s, x, TOP + 0.1, chev_w, 1.35, c, MSO_SHAPE.CHEVRON)
    tf = sp.text_frame
    tf.word_wrap = True
    for j, ln in enumerate(st.split("\n")):
        p = tf.paragraphs[0] if j == 0 else tf.add_paragraph()
        p.alignment = PP_ALIGN.CENTER
        r = p.add_run()
        r.text = ln
        F(r, 15, WHITE, True)
bullets(s, [
    "攻坚 30+ 典型问题：H2 保留字建表 · 九宫格解析 · XSS 转义 · 越权修复",
    "全链路留痕：设计文档 · 审查报告 · 测试文档 · 运行截图",
    "提示词资产沉淀：可复用、可复盘、可交接",
], MX, 4.1, CW, size=15.5, gap=14)
footer(s, 13)

# ================= P13 测试与质量 =================
s = page("05 · 质量与实践", "测试与质量", "用数据说话")
rect(s, MX, TOP, 3.9, 4.55, NAVY)
tb(s, MX, TOP + 0.35, 3.9, 0.9, "51", size=52, color=WHITE, bold=True, align=PP_ALIGN.CENTER)
tb(s, MX, TOP + 1.35, 3.9, 0.4, "接口测试用例", size=14, color=AMBER, align=PP_ALIGN.CENTER)
rect(s, MX + 1.45, TOP + 1.95, 1.0, 0.03, AMBER)
tb(s, MX, TOP + 2.2, 3.9, 0.8, "92%", size=44, color=WHITE, bold=True, align=PP_ALIGN.CENTER)
tb(s, MX, TOP + 3.05, 3.9, 0.4, "通过率（含权限/异常）", size=13, color=SKY, align=PP_ALIGN.CENTER)
tb(s, MX, TOP + 3.75, 3.9, 0.6, "唯一未过项 = 缺参返回 400\n属预期参数校验", size=12,
   color=RGBColor(0x9F, 0xB0, 0xD8), align=PP_ALIGN.CENTER)
rows = [
    ("功能用例 · 40", "认证/组织/档案/考勤/薪酬/绩效/培训/招聘/自助/分析/系统"),
    ("权限用例 · 5", "四种角色越权访问，全部返回 403"),
    ("异常用例 · 6", "401 未登录 · 403 越权 · 400 缺参 · 404 资源不存在"),
    ("构建校验", "前端 vue-tsc 全量构建 · 零类型告警"),
]
yy = TOP
for t, d in rows:
    rect(s, 5.0, yy, 7.63, 1.02, LIGHT, MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.1)
    rect(s, 5.0, yy, 0.09, 1.02, BLUE)
    tb(s, 5.3, yy + 0.14, 7.2, 0.36, t, size=15, color=NAVY, bold=True)
    tb(s, 5.3, yy + 0.52, 7.2, 0.4, d, size=12.5, color=GRAY)
    yy += 1.19
footer(s, 14)

# ================= P15 实习反馈 =================
s = page("06 · 总结与展望", "实习反馈", "从写代码到做工程")
card(s, MX, TOP, 3.9, 4.55, "技术成长",
     "全栈贯通：Spring Boot 3 + Vue3\n\n工程化：一键启动 · 回归脚本 · 云端部署\n\n安全内建：JWT · RBAC · XSS · CORS", size=13.5)
card(s, 4.72, TOP, 3.9, 4.55, "AI 协作方式",
     "提示词驱动：设计 → 代码 → 验证\n\n多代理并行：60+ 接口自动回归\n\n全程留痕：46 次提交可复盘", size=13.5, tcolor=RGBColor(0xB4, 0x78, 0x00))
card(s, 8.74, TOP, 3.9, 4.55, "实践感悟",
     "设计先行，返工更少\n\n小步提交 + 自动验证更踏实\n\n部署上线，成果随时可演示", size=13.5, tcolor=GREEN)
rect(s, MX, 6.35, CW, 0.62, NAVY, MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.5)
tb(s, MX, 6.35, CW, 0.62, "最大收获：把「会写功能」升级为「会交付产品」",
   size=15, color=WHITE, bold=True, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
footer(s, 15)

# ================= P16 总结与展望 =================
s = page("06 · 总结与展望", "成果 · 创新 · 展望", "可交付 · 可复现 · 可演进")
card(s, MX, TOP, 3.9, 4.55, "成果",
     "可运行的全栈代码\n\n8 份文档（设计·测试·手册）\n\n运行截图 + 操作手册\n\n答辩 PPT + 一键启动", size=14)
card(s, 4.72, TOP, 3.9, 4.55, "创新",
     "AI 智能问数\n\n数据能力沉淀复用\n\n幂等一键算薪\n\n固定种子可复现演示", size=14, tcolor=RGBColor(0xB4, 0x78, 0x00))
card(s, 8.74, TOP, 3.9, 4.55, "展望",
     "聊天历史持久化\n\n自动化单元测试\n\n物理外键与事务\n\n接入大模型增强问数", size=14, tcolor=GREEN)
footer(s, 16)

# ================= P15 致谢 =================
s = prs.slides.add_slide(BLANK)
rect(s, 0, 0, 13.333, 7.5, NAVY)
rect(s, 0, 0, 13.333, 0.14, BLUE)
rect(s, 0, 7.36, 13.333, 0.14, BLUE)
tb(s, 1.0, 2.7, 11.3, 1.1, "谢谢聆听 · Q&A", size=52, color=WHITE, bold=True, align=PP_ALIGN.CENTER)
rect(s, 5.55, 4.05, 2.2, 0.05, AMBER)
tb(s, 1.0, 4.45, 11.3, 0.5, "企业员工管理系统 HRMS", size=18, color=SKY, align=PP_ALIGN.CENTER)
tb(s, 1.0, 5.3, 11.3, 0.5, "代码仓库：Gitee · 欢迎提问", size=14,
   color=RGBColor(0x9F, 0xB0, 0xD8), align=PP_ALIGN.CENTER)

prs.save(OUT)
print("OK:", OUT, "| slides:", len(prs.slides._sldIdLst))
