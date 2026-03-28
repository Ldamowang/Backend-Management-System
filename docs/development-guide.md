# 开发指南

## 目录

- [开发环境搭建](#开发环境搭建)
- [项目结构](#项目结构)
- [前端开发](#前端开发)
- [后端开发](#后端开发)
- [开发规范](#开发规范)
- [调试技巧](#调试技巧)

---

## 开发环境搭建

### 前置依赖

| 工具 | 版本 | 用途 |
|------|------|------|
| JDK | 17 | 后端编译运行 |
| Maven | 3.8+ | 后端依赖管理 |
| Node.js | 18+ | 前端开发 |
| npm | 9+ | 前端包管理 |
| MySQL | 8.0 | 数据库 |
| Redis | 7.0+ | 缓存 |
| Git | 2.30+ | 版本控制 |

### 1. 启动基础服务

推荐使用 Docker 快速启动 MySQL 和 Redis:

```bash
# MySQL
docker run -d --name admin-mysql \
  -e MYSQL_ROOT_PASSWORD=root123456 \
  -e MYSQL_DATABASE=admin_db \
  -p 3306:3306 \
  mysql:8.0

# Redis
docker run -d --name admin-redis \
  -p 6379:6379 \
  redis:7-alpine
```

### 2. 初始化数据库

```bash
mysql -h 127.0.0.1 -u root -proot123456 admin_db < sql/schema.sql
mysql -h 127.0.0.1 -u root -proot123456 admin_db < sql/data.sql
```

### 3. 启动后端

```bash
cd admin-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

后端启动后可访问:
- API: http://localhost:8080/api
- Swagger: http://localhost:8080/swagger-ui.html

### 4. 启动前端

```bash
cd admin-frontend
npm install
npm run dev
```

前端启动后访问 http://localhost:5173，Vite 会自动代理 `/api` 请求到后端 8080 端口。

---

## 项目结构

### 前端

```
admin-frontend/src/
├── api/                # API 请求层
│   ├── request.ts      #   Axios 实例 + 拦截器
│   └── modules/        #   按模块拆分的 API 函数
├── assets/             # 静态资源
│   └── styles/         #   全局样式 + SCSS 变量
├── components/         # 全局公共组件
├── composables/        # 组合式函数 (Hooks)
├── directives/         # 自定义指令 (v-permission)
├── plugins/            # 插件配置 (Element Plus)
├── router/             # 路由配置 (静态 + 动态)
├── stores/             # Pinia 状态管理
│   └── modules/        #   按模块拆分的 Store
├── types/              # TypeScript 类型定义
├── utils/              # 工具函数
└── views/              # 页面视图 (按路由组织)
```

### 后端

```
admin-backend/src/main/java/com/iflytek/admin/
├── AdminApplication.java     # 启动类
├── base/                     # 基础实体类
├── common/
│   ├── annotation/           #   自定义注解 (@Log)
│   ├── aspect/               #   AOP 切面 (日志记录)
│   ├── config/               #   Spring 配置类
│   ├── constant/             #   常量定义
│   ├── enums/                #   枚举类
│   ├── exception/            #   异常类 + 全局异常处理
│   ├── result/               #   统一响应封装
│   └── utils/                #   工具类
├── security/                 # Spring Security + JWT
└── modules/                  # 业务模块
    ├── auth/                 #   认证 (登录/登出/刷新)
    ├── system/               #   系统管理 (用户/角色/菜单/配置/日志)
    └── profile/              #   个人中心
```

---

## 前端开发

### 新增页面步骤

1. **创建视图**: `src/views/模块名/页面名/index.vue`
2. **创建 API**: `src/api/modules/模块名.ts`
3. **添加路由**: 在后台菜单管理中添加菜单 (动态路由)
4. **添加类型**: `src/types/模块名.ts` (如需要)

### API 调用示例

```typescript
// src/api/modules/example.ts
import request from '@/api/request'

// 列表查询
export function getExampleList(params: ExampleQuery) {
  return request.get('/system/examples', { params })
}

// 新增
export function createExample(data: ExampleForm) {
  return request.post('/system/examples', data)
}

// 修改
export function updateExample(id: number, data: ExampleForm) {
  return request.put(`/system/examples/${id}`, data)
}

// 删除
export function deleteExample(id: number) {
  return request.delete(`/system/examples/${id}`)
}
```

### 权限控制

**路由权限**: 通过菜单配置自动生成动态路由，无权限的菜单不会出现在侧边栏。

**按钮权限**: 使用 `v-permission` 指令:

```vue
<template>
  <el-button v-permission="'sys:user:add'">新增</el-button>
  <el-button v-permission="'sys:user:edit'">编辑</el-button>
  <el-button v-permission="'sys:user:delete'" type="danger">删除</el-button>
</template>
```

### 状态管理

使用 Pinia 按模块管理状态:

```typescript
// src/stores/modules/example.ts
import { defineStore } from 'pinia'

export const useExampleStore = defineStore('example', () => {
  const list = ref<Example[]>([])

  async function fetchList() {
    const res = await getExampleList({})
    list.value = res.data.list
  }

  return { list, fetchList }
})
```

### 前端命令

```bash
npm run dev              # 启动开发服务器
npm run build            # 生产构建 (含类型检查)
npm run type-check       # 仅 TypeScript 类型检查
npm run lint             # ESLint 检查 + 自动修复
npm run test             # 运行单元测试
npm run test:watch       # 监听模式
npm run test:coverage    # 生成覆盖率报告
npm run test:e2e         # Playwright E2E 测试
```

---

## 后端开发

### 新增模块步骤

1. **创建实体**: `modules/模块名/entity/XxxEntity.java`
2. **创建 DTO**: `modules/模块名/dto/XxxDTO.java`、`XxxQuery.java`
3. **创建 Converter**: `modules/模块名/converter/XxxConverter.java` (MapStruct)
4. **创建 Mapper**: `modules/模块名/mapper/XxxMapper.java` (MyBatis-Plus)
5. **创建 Service**: `modules/模块名/service/XxxService.java` + `impl/`
6. **创建 Controller**: `modules/模块名/controller/XxxController.java`
7. **添加 SQL**: `sql/` 下新增迁移脚本

### Controller 示例

```java
@RestController
@RequestMapping("/api/system/examples")
@RequiredArgsConstructor
public class ExampleController {

    private final ExampleService exampleService;

    @GetMapping
    @PreAuthorize("hasAuthority('sys:example:list')")
    public Result<PageResult<ExampleDTO>> list(ExampleQuery query) {
        return Result.success(exampleService.page(query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sys:example:add')")
    @Log(module = "示例管理", operation = "新增")
    public Result<Void> create(@RequestBody @Valid ExampleCreateDTO dto) {
        exampleService.create(dto);
        return Result.success();
    }
}
```

### 数据库约定

| 约定 | 规则 |
|------|------|
| 表名 | `sys_` 前缀，小写下划线 |
| 主键 | `id` BIGINT AUTO_INCREMENT |
| 逻辑删除 | `deleted` TINYINT (0=正常, 1=删除) |
| 审计字段 | `created_by`, `created_time`, `updated_by`, `updated_time` |
| 时间类型 | DATETIME |
| 字符集 | utf8mb4_unicode_ci |

### 后端命令

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev   # 启动开发模式
mvn compile                                            # 编译
mvn test                                               # 运行测试
mvn clean package                                      # 打包 JAR
mvn clean package -DskipTests                          # 跳过测试打包
```

---

## 开发规范

### Git 提交规范

```
<type>: <description>

<optional body>
```

| Type | 用途 |
|------|------|
| feat | 新功能 |
| fix | 修复 Bug |
| refactor | 重构 (不改变功能) |
| docs | 文档 |
| test | 测试 |
| chore | 构建/工具/依赖 |
| perf | 性能优化 |
| ci | CI/CD 配置 |

### 分支策略

| 分支 | 用途 |
|------|------|
| `main` | 生产分支，保持稳定 |
| `develop` | 开发分支，合并功能 |
| `feature/*` | 功能分支，从 develop 创建 |
| `hotfix/*` | 紧急修复，从 main 创建 |

### API 设计规范

- RESTful 风格: GET(查), POST(增), PUT(改), DELETE(删), PATCH(部分更新)
- 资源路径使用复数: `/api/system/users`
- 统一响应格式: `{ code, message, data }`
- 权限标识格式: `module:entity:action` (如 `sys:user:add`)

---

## 调试技巧

### 前端

- **Vue DevTools**: 浏览器安装 Vue.js devtools 扩展，查看组件树和 Pinia 状态
- **Vite HMR**: 代码修改后自动热更新，无需刷新
- **网络请求**: 浏览器 DevTools → Network 标签页查看 API 请求/响应
- **代理日志**: Vite 开发服务器会在终端输出代理的请求日志

### 后端

- **Swagger UI**: http://localhost:8080/swagger-ui.html 直接测试 API
- **SQL 日志**: `application-dev.yml` 中已配置 MyBatis-Plus SQL 日志输出
- **Debug 模式**: IDE 中以 Debug 模式启动 Spring Boot，设置断点调试
- **日志级别**: `application.yml` 中 `logging.level.com.iflytek.admin: debug`

### 数据库

```bash
# 连接数据库
mysql -h 127.0.0.1 -u root -proot123456 admin_db

# 常用查询
SELECT * FROM sys_user WHERE deleted = 0;
SELECT * FROM sys_role;
SELECT m.* FROM sys_menu m ORDER BY parent_id, sort;
```
