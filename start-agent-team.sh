#!/bin/bash
#
# Agent Team Launcher - 使用 tmux 分屏启动多个 Claude Code Agent
# 每个 Agent 负责项目的不同方面，并行工作
#

PROJECT_DIR="/Users/yili/Desktop/iflytek/test-demo/code"
SESSION_NAME="agent-team"
PROMPTS_DIR="$PROJECT_DIR/.agent-prompts"

# 颜色输出
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== Agent Team Launcher ===${NC}"
echo -e "${BLUE}项目目录: ${PROJECT_DIR}${NC}"
echo ""

# 如果已存在同名 session，先结束
tmux has-session -t "$SESSION_NAME" 2>/dev/null && {
    echo -e "${YELLOW}发现已存在的 session '$SESSION_NAME'，正在清理...${NC}"
    tmux kill-session -t "$SESSION_NAME"
}

# ========== 创建 tmux 会话 ==========

echo -e "${GREEN}正在创建 tmux 会话: ${SESSION_NAME}${NC}"

# 创建新 session，第一个窗口给 Agent 1（后端测试）
tmux new-session -d -s "$SESSION_NAME" -n "agents" -x 200 -y 50

# 水平分割 - 上下两行
tmux split-window -v -t "$SESSION_NAME:agents"

# 左上角再垂直分割 - 左右两列（上面一行）
tmux split-window -h -t "$SESSION_NAME:agents.0"

# 左下角再垂直分割 - 左右两列（下面一行）
tmux split-window -h -t "$SESSION_NAME:agents.2"

# 现在有 4 个 pane：
# Pane 0: 左上 - Agent 1 (后端测试)
# Pane 1: 右上 - Agent 2 (前端质量)
# Pane 2: 左下 - Agent 3 (安全审查)
# Pane 3: 右下 - Agent 4 (CI/CD & 文档)

# 设置 pane 标题
tmux select-pane -t "$SESSION_NAME:agents.0" -T "Backend-Test-Agent"
tmux select-pane -t "$SESSION_NAME:agents.1" -T "Frontend-QA-Agent"
tmux select-pane -t "$SESSION_NAME:agents.2" -T "Security-Agent"
tmux select-pane -t "$SESSION_NAME:agents.3" -T "CICD-Docs-Agent"

# 显示 pane 边框标题
tmux set-option -t "$SESSION_NAME" pane-border-status top
tmux set-option -t "$SESSION_NAME" pane-border-format " #{pane_title} "

# ========== 启动各 Agent（使用独立脚本，避免环境和转义问题） ==========

echo -e "${BLUE}启动 Agent 1: 后端测试 Agent${NC}"
tmux send-keys -t "$SESSION_NAME:agents.0" "bash $PROMPTS_DIR/agent1-backend-test.sh" Enter

echo -e "${BLUE}启动 Agent 2: 前端质量 Agent${NC}"
tmux send-keys -t "$SESSION_NAME:agents.1" "bash $PROMPTS_DIR/agent2-frontend-qa.sh" Enter

echo -e "${BLUE}启动 Agent 3: 安全审查 Agent${NC}"
tmux send-keys -t "$SESSION_NAME:agents.2" "bash $PROMPTS_DIR/agent3-security.sh" Enter

echo -e "${BLUE}启动 Agent 4: CI/CD & 文档 Agent${NC}"
tmux send-keys -t "$SESSION_NAME:agents.3" "bash $PROMPTS_DIR/agent4-cicd-docs.sh" Enter

echo ""
echo -e "${GREEN}=== Agent Team 已启动! ===${NC}"
echo ""
echo -e "  tmux session: ${YELLOW}${SESSION_NAME}${NC}"
echo ""
echo -e "  ┌─────────────────────┬─────────────────────┐"
echo -e "  │  ${BLUE}Backend-Test-Agent${NC}  │  ${BLUE}Frontend-QA-Agent${NC}   │"
echo -e "  │  后端测试补充        │  前端质量检查         │"
echo -e "  ├─────────────────────┼─────────────────────┤"
echo -e "  │  ${BLUE}Security-Agent${NC}      │  ${BLUE}CICD-Docs-Agent${NC}    │"
echo -e "  │  安全审查            │  CI/CD & 文档        │"
echo -e "  └─────────────────────┴─────────────────────┘"
echo ""
echo -e "  操作提示:"
echo -e "    ${YELLOW}tmux attach -t $SESSION_NAME${NC}     进入会话查看"
echo -e "    ${YELLOW}Ctrl+B 然后方向键${NC}               切换 pane"
echo -e "    ${YELLOW}Ctrl+B 然后 z${NC}                   全屏/还原当前 pane"
echo -e "    ${YELLOW}Ctrl+B 然后 d${NC}                   后台运行（detach）"
echo -e "    ${YELLOW}tmux kill-session -t $SESSION_NAME${NC}  停止所有 Agent"
echo ""
