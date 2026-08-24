#!/usr/bin/env bash
# ==========================================================
# HRMS 企业员工管理系统 — CloudStudio 一键启动脚本
# 自动构建/安装依赖并同时启动前后端服务（后端 H2 内嵌库，免装 MySQL）
# 支持 apt-get / yum / apk 包管理器；非 root 时自动降级使用 sudo
# ==========================================================
set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

APP_NAME="🏢 企业员工管理系统 (HRMS)"

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}  ${APP_NAME} — CloudStudio 全栈启动 ${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$PROJECT_DIR/backend"
FRONTEND_DIR="$PROJECT_DIR/frontend"

# --------------- 包管理器探测 ---------------
# PM：实际命令（自动补 sudo）；PM_UPDATE：包索引刷新
if command -v apt-get >/dev/null 2>&1; then
  PM_BASE="apt-get"; PM_UPDATE="apt-get update -qq"; PM_INSTALL="apt-get install -y"
elif command -v yum >/dev/null 2>&1; then
  PM_BASE="yum"; PM_UPDATE="yum makecache"; PM_INSTALL="yum install -y"
elif command -v apk >/dev/null 2>&1; then
  PM_BASE="apk"; PM_UPDATE="apk update"; PM_INSTALL="apk add"
else
  echo -e "${YELLOW}[警告] 未识别包管理器，跳过自动安装（将依赖环境预装）。${NC}"
  PM_BASE=""; PM_UPDATE=""; PM_INSTALL=""
fi
if [ -n "$PM_BASE" ] && [ "$(id -u)" -ne 0 ]; then
  PM_BASE="sudo $PM_BASE"
fi

PM_INSTALL_CMD="$PM_INSTALL"
PM_UPDATE_CMD="$PM_UPDATE"
APT_RAN_UPDATE=0

# 通用：缺失依赖则按包管理器挑选对应包名安装；装不上返回非零
# 用法：ensure_cmd <命令名> <apt包名> <yum包名> <apk包名>
ensure_cmd() {
  local name=$1; local apt_pkg=$2; local yum_pkg=$3; local apk_pkg=$4
  if command -v "$name" >/dev/null 2>&1; then return 0; fi
  local pkg=""
  case "$PM_BASE" in
    *apt-get*) pkg=$apt_pkg ;;
    *yum*)     pkg=$yum_pkg ;;
    *apk*)     pkg=$apk_pkg ;;
  esac
  [ -z "$pkg" ] && { echo -e "${YELLOW}  缺少工具：${name}，且无候选安装包，请手动安装${NC}"; return 1; }
  echo -e "${YELLOW}  安装缺失工具：$name ...${NC}"
  if [ "$APT_RAN_UPDATE" -eq 0 ]; then
    $PM_UPDATE_CMD >/dev/null 2>&1 || true
    APT_RAN_UPDATE=1
  fi
  $PM_INSTALL_CMD "$pkg" >/dev/null 2>&1
  command -v "$name" >/dev/null 2>&1
}

# --------------- 前置工具检查 ---------------
echo -e "${YELLOW}检查运行环境...${NC}"

# curl
ensure_cmd curl curl curl curl || { echo "请先安装 curl"; exit 1; }

# Java 17（后端硬依赖）
JAVA_BIN="java"
[ -n "$JAVA_HOME" ] && JAVA_BIN="$JAVA_HOME/bin/java"
if ! command -v "$JAVA_BIN" >/dev/null 2>&1; then
  ensure_cmd java openjdk-17-jre-headless java-17-openjdk-headless openjdk17 \
    || { echo -e "${YELLOW}无法自动安装 JDK17，请手动安装后重试${NC}"; exit 1; }
fi
JAVA_VER=$("$JAVA_BIN" -version 2>&1 | head -n1 | sed -E 's/.*[^0-9]([0-9]+).*/\1/')
if [ "${JAVA_VER:-0}" -lt 17 ]; then
  echo -e "${YELLOW}  Java 版本过低（$JAVA_VER，需 ≥17），请配置 JDK17${NC}"
  exit 1
fi
echo -e "${GREEN}  Java $JAVA_VER ✓${NC}"

# Node / npm（前端硬依赖）
if ! command -v node >/dev/null 2>&1 || ! command -v npm >/dev/null 2>&1; then
  ensure_cmd node nodejs nodejs nodejs \
    || { echo -e "${YELLOW}无法自动安装 Node，请手动安装 Node ≥18 后重试${NC}"; exit 1; }
fi
NODE_MAJOR=$(node -v | sed -E 's/^v([0-9]+).*/\1/')
if [ "${NODE_MAJOR:-0}" -lt 18 ]; then
  echo -e "${YELLOW}  Node 版本过低（$NODE_MAJOR，Vite5 需 ≥18 ），请升级 Node${NC}"
  exit 1
fi
echo -e "${GREEN}  Node ${NODE_MAJOR} ✓${NC}"

# --------------- jar 是否需要重新构建 ---------------
need_build() {
  local jar="$BACKEND_DIR/target/hrms-backend-1.0.0.jar"
  [ -f "$jar" ] || return 0
  if find "$BACKEND_DIR/src" "$BACKEND_DIR/pom.xml" -newer "$jar" 2>/dev/null | grep -q .; then
    return 0
  fi
  return 1
}

# --------------- 1/3: 构建并启动后端 (端口 8000) ---------------
echo -e "${YELLOW}[1/3] 构建后端 jar...${NC}"
cd "$BACKEND_DIR"
if need_build; then
  if ! command -v mvn >/dev/null 2>&1 && [ ! -f "$BACKEND_DIR/mvnw" ]; then
    ensure_cmd mvn maven maven maven \
      || { echo -e "${YELLOW}无法自动安装 Maven，请手动安装后重试${NC}"; exit 1; }
  fi
  if [ -f "$BACKEND_DIR/mvnw" ]; then
    "$BACKEND_DIR/mvnw" clean package -DskipTests -q
  else
    mvn clean package -DskipTests -q
  fi
fi

echo -e "${YELLOW}[2/3] 启动后端服务 (H2 内嵌库, 端口 8000)...${NC}"
"$JAVA_BIN" -jar "$BACKEND_DIR/target/hrms-backend-1.0.0.jar" &
BACKEND_PID=$!
echo -e "${GREEN}  后端 PID: $BACKEND_PID${NC}"

# 等待后端就绪（轮询登录接口，最长 120s）
READY=0
for _ in $(seq 1 120); do
  if curl -s -o /dev/null -w '%{http_code}' http://localhost:8000/api/auth/login \
      -X POST -H 'Content-Type: application/json' -d '{"username":"x","password":"x"}' 2>/dev/null \
      | grep -qE '200|401'; then
    READY=1; break
  fi
  sleep 1
done
[ "$READY" -eq 1 ] || { echo -e "${YELLOW}后端启动超时，请查看上方日志${NC}"; kill "$BACKEND_PID" 2>/dev/null || true; exit 1; }
echo -e "${GREEN}  后端已就绪。${NC}"

# --------------- 3/3: 安装并启动前端 (端口 3000) ---------------
echo -e "${YELLOW}[3/3] 安装前端依赖...${NC}"
cd "$FRONTEND_DIR"
if [ ! -d "node_modules" ]; then
  npm install --no-audit --no-fund
fi

echo -e "${GREEN}  启动前端开发服务器 (端口 3000)...${NC}"
# 云IDE 公网预览下关闭 HMR，避免 ws 连 localhost 触发浏览器 PNA 拦截
export VITE_NO_HMR=1
npx vite --host 0.0.0.0 --port 3000 &
FRONTEND_PID=$!
echo -e "${GREEN}  前端 PID: $FRONTEND_PID${NC}"

# --------------- 完成 ---------------
echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${GREEN}  ✅ ${APP_NAME} 已启动！${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""
echo -e "  📡 前端地址:  ${GREEN}http://localhost:3000${NC}"
echo -e "  📡 后端地址:  ${GREEN}http://localhost:8000${NC}"
echo -e "  📖 H2 控制台:  ${GREEN}http://localhost:8000/h2-console${NC}"
echo ""
echo -e "  ${YELLOW}💡 CloudStudio 会自动检测打开的端口，${NC}"
echo -e "  ${YELLOW}   生成可公开访问的预览链接。${NC}"
echo ""
echo -e "  ${YELLOW}演示账号: admin / 123456（另有 hr / manager1 / employee1，密码均 123456）${NC}"
echo ""
echo -e "  ${YELLOW}按 Ctrl+C 停止所有服务${NC}"
echo ""

cleanup() {
  echo ""
  echo -e "${YELLOW}正在停止服务...${NC}"
  kill "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true
  wait "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true
  echo -e "${GREEN}服务已停止。${NC}"
  exit 0
}
trap cleanup SIGINT SIGTERM

wait