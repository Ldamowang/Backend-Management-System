# 语言包规范

## 新增语种

1. 在 `src/locales/` 下创建语言目录（如 `ja-JP/`）
2. 按照 `zh-CN/` 的结构创建同名 JSON 文件，key 保持一致
3. 在 `src/locales/index.ts` 的 `loadLanguage` 函数中添加新语种的懒加载逻辑
4. 在 `availableLocales` 数组中注册新语种

## 文件结构

| 文件 | 内容 |
|------|------|
| common.json | 通用文本：按钮、标签、消息、分页、设置、标签页、搜索 |
| menu.json | 侧边栏菜单名称 |
| login.json | 登录页文本 |
| dashboard.json | 仪表盘页文本 |
| system.json | 系统管理所有子模块文本 |
| profile.json | 个人中心文本 |

## 使用方式

模板中：`{{ $t('common.action.confirm') }}`
脚本中：`const { t } = useI18n(); t('common.action.confirm')`

## 注意事项

- 所有 key 必须在 zh-CN 中有对应值（fallback 语言）
- 支持插值：`"共 {total} 条"` → `$t('common.label.total', { total: 100 })`
- 后端返回的动态数据不做翻译
