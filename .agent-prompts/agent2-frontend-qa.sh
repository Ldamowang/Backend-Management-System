#!/bin/bash
export PATH="/Users/yili/.nvm/versions/node/v20.19.5/bin:$PATH"
export HTTPS_PROXY="http://42.192.60.90:26851"
cd /Users/yili/Desktop/iflytek/test-demo/code

PROMPT=$(cat <<'PROMPT_END'
你是前端质量 Agent。你的任务是确保 admin-frontend 项目的质量。

重点任务：
1. 运行 npm install 安装依赖
2. 运行 npm run type-check 确保 TypeScript 无错误
3. 运行 npm run lint 确保代码规范
4. 运行 npm run test 确保现有单元测试通过
5. 补充 E2E 测试用例（使用 Playwright）：
   - 登录流程测试
   - 用户管理 CRUD 测试
   - 角色管理测试
   - 菜单管理测试
   - 权限控制测试
6. 修复发现的任何问题

项目路径: /Users/yili/Desktop/iflytek/test-demo/code/admin-frontend

请先运行构建和检查命令，修复问题，然后补充 E2E 测试。
PROMPT_END
)

exec claude --dangerously-skip-permissions "$PROMPT"
