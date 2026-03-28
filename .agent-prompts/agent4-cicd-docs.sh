#!/bin/bash
export PATH="/Users/yili/.nvm/versions/node/v20.19.5/bin:$PATH"
export HTTPS_PROXY="http://42.192.60.90:26851"
cd /Users/yili/Desktop/iflytek/test-demo/code

PROMPT=$(cat <<'PROMPT_END'
你是 CI/CD 和文档 Agent。你的任务是为项目添加 CI/CD 配置和完善文档。

重点任务：
1. 创建 GitHub Actions CI/CD 配置：
   - 前端：install -> lint -> type-check -> test -> build
   - 后端：compile -> test -> package
   - Docker 镜像构建
   - 部署流程
2. 创建 .env.example 文件（Docker 部署环境变量模板）
3. 完善 docs/ 目录下的项目文档：
   - API 文档
   - 部署指南
   - 开发指南

项目路径: /Users/yili/Desktop/iflytek/test-demo/code

请先了解项目结构，然后按优先级创建 CI/CD 配置和文档。
PROMPT_END
)

exec claude --dangerously-skip-permissions "$PROMPT"
