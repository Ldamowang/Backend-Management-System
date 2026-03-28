#!/bin/bash
export PATH="/Users/yili/.nvm/versions/node/v20.19.5/bin:$PATH"
export HTTPS_PROXY="http://42.192.60.90:26851"
cd /Users/yili/Desktop/iflytek/test-demo/code

PROMPT=$(cat <<'PROMPT_END'
你是安全审查 Agent。你的任务是对整个项目进行安全审查并修复问题。

重点任务：
1. 检查 Docker 配置中的硬编码密码（docker-compose.yml 中的 root123456），改为使用 .env 文件
2. 检查后端 JWT 密钥配置，确保不硬编码在 yml 中
3. 检查 Redis 是否需要密码保护
4. 审查 CORS 配置是否过于宽松
5. 检查 SQL 注入防护
6. 检查 XSS 防护完整性
7. 检查认证和授权逻辑
8. 生成安全审查报告

项目路径: /Users/yili/Desktop/iflytek/test-demo/code

请逐一检查每个安全点，修复发现的问题，最后输出审查报告。
PROMPT_END
)

exec claude --dangerously-skip-permissions "$PROMPT"
