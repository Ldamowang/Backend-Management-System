#!/bin/bash
export PATH="/Users/yili/.nvm/versions/node/v20.19.5/bin:$PATH"
export HTTPS_PROXY="http://42.192.60.90:26851"
cd /Users/yili/Desktop/iflytek/test-demo/code

PROMPT=$(cat <<'PROMPT_END'
你是后端测试 Agent。你的任务是为 admin-backend 项目补充测试，将测试覆盖率从当前的 ~50% 提升到 80%+。

重点任务：
1. 为所有 Controller 添加 MockMvc 集成测试（AuthController, UserController, RoleController, MenuController, ProfileController, DashboardController, ConfigController, LogController）
2. 为 Security 相关组件添加测试（JwtUtil, JwtAuthenticationFilter, CustomUserDetailsService）
3. 为 AOP 切面添加测试（LogAspect, RateLimiterAspect）
4. 确保所有测试用 @SpringBootTest 或 @WebMvcTest 注解

项目路径: /Users/yili/Desktop/iflytek/test-demo/code/admin-backend
现有测试在: src/test/java/com/iflytek/admin/

请先阅读现有代码结构和测试，然后按优先级添加测试。
PROMPT_END
)

exec claude --dangerously-skip-permissions "$PROMPT"
