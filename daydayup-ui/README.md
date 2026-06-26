# DayDayUP 前端工程 (daydayup-ui)

基于 pnpm workspace 的 Monorepo 前端工程，为 DayDayUP 微服务系统提供管理后台界面。

## 项目概览

- **技术栈**: Vue 3 + Vite + TypeScript + Pinia + Element Plus + Vue Router
- **包管理**: pnpm workspace
- **架构模式**: Monorepo 多包协作
- **Node 版本**: >=20.0.0

## 目录结构

```
daydayup-ui/
├── packages/
│   ├── shared/                    # 共享基础库
│   │   ├── src/
│   │   │   ├── api/              # HTTP 客户端封装
│   │   │   ├── auth/             # 认证模块
│   │   │   ├── utils/            # 工具函数
│   │   │   ├── types/            # TypeScript 类型定义
│   │   │   └── constants/        # 常量定义
│   │   ├── vitest.config.ts
│   │   └── package.json
│   │
│   └── apps/
│       └── admin-ui/              # 管理后台应用
│           ├── src/
│           │   ├── layouts/      # 布局组件
│           │   ├── views/        # 页面视图
│           │   ├── router/       # 路由配置
│           │   ├── stores/       # Pinia 状态管理
│           │   ├── App.vue       # 根组件
│           │   └── main.ts       # 入口文件
│           ├── vite.config.ts
│           ├── .env.development
│           ├── .env.production
│           └── package.json
│
├── pnpm-workspace.yaml
└── package.json
```

## 快速开始

### 安装依赖

```bash
cd daydayup-ui
npm install -g pnpm  # 如果未安装 pnpm
pnpm install
```

### 开发模式

启动管理后台开发服务器：

```bash
pnpm dev:admin
```

访问 `http://127.0.0.1:5173`

### 构建生产版本

```bash
# 构建管理后台
pnpm build:admin

# 构建 shared 包（类型检查）
pnpm build:shared
```

### 运行测试

```bash
# 运行 shared 包单元测试
pnpm test:shared
```

## 核心功能

### @daydayup/shared 包

提供多应用共享的基础设施：

#### 1. HTTP Client
- 基于 Axios 的统一请求封装
- 自动 Token 注入
- 请求/响应拦截器
- 统一错误处理
- 自动 Token 刷新
- 接口签名支持（HmacSHA256）

```typescript
import { createHttpClient } from '@daydayup/shared';

const httpClient = createHttpClient('http://127.0.0.1:9000');
httpClient.get('/api/users');
```

#### 2. 认证模块
- Token 存储管理（sessionStorage + Base64 混淆）
- DirectToken 登录流程
- Token 刷新机制

```typescript
import { tokenStore, createAuthApi } from '@daydayup/shared';

const authApi = createAuthApi(httpClient);
await authApi.login({ username, password });
```

#### 3. 工具函数库
- **字符串**: `isEmpty`, `capitalize`, `truncate`, `camelToKebab`
- **数组**: `unique`, `groupBy`, `chunk`, `flatten`
- **日期**: `formatDate`, `getRelativeTime`, `isToday`
- **存储**: `localStorageHelper`, `sessionStorageHelper`
- **验证**: `isEmail`, `isMobile`, `isUrl`, `isStrongPassword`
- **权限**: `can`, `hasAnyPermission`, `hasAllPermissions`

#### 4. TypeScript 类型
- 用户类型 (`UserDetail`, `UserQuery`, `UserForm`)
- 菜单类型 (`MenuItem`, `MenuQuery`, `MenuForm`)
- 字典类型 (`Dict`, `DictItem`)
- 操作日志 (`OperLog`)
- API 响应 (`ApiResponse`, `PageResult`)

### @daydayup/admin-ui 应用

管理后台单页应用：

#### 功能模块
- ✅ 登录页面（账号密码登录）
- ✅ 首页仪表盘
- ✅ 用户管理（占位页面）
- ✅ 菜单管理（占位页面）
- ✅ 字典管理（占位页面）
- ✅ 字典项管理（占位页面）
- ✅ 操作日志（占位页面）
- ✅ API 密钥管理（占位页面）
- ✅ 403/404 错误页面

#### 路由守卫
- 自动登录状态检查
- Token 过期自动跳转
- 权限验证（基于 `admin:*` 通配符）
- 用户信息自动获取

#### 状态管理
- **authStore**: 登录、登出、Token 管理
- **userStore**: 用户信息、权限管理
- **appStore**: 侧边栏、主题、语言设置

## 环境变量

### 开发环境 (`.env.development`)

```env
VITE_GATEWAY_BASE_URL=http://127.0.0.1:9000
VITE_AUTH_BASE_URL=http://127.0.0.1:9200
VITE_APP_TITLE=DayDayUP 管理后台
VITE_API_SECRET=dev-secret-key
```

### 生产环境 (`.env.production`)

```env
VITE_GATEWAY_BASE_URL=https://api.daydayup.com
VITE_AUTH_BASE_URL=https://auth.daydayup.com
VITE_APP_TITLE=DayDayUP 管理后台
VITE_API_SECRET=prod-secret-key
```

## 测试

### 测试覆盖

- ✅ HTTP Client 测试（9 个测试用例）
- ✅ Token Store 测试（11 个测试用例）
- ✅ 数学工具测试（1 个测试用例）

总计：**21 个测试用例全部通过**

### 运行测试

```bash
# 运行所有测试
pnpm test:shared

# 类型检查
pnpm build:shared
```

## 技术亮点

1. **Monorepo 架构**: 多包协作，代码复用，统一依赖管理
2. **TypeScript 严格模式**: 全面的类型安全和编译时检查
3. **自动 Token 刷新**: 无感知的登录态延续
4. **权限通配符**: 支持 `admin:*` 匹配所有 `admin:` 开头的权限
5. **Element Plus 集成**: 开箱即用的 UI 组件库
6. **Vite 代理**: 开发环境跨域代理配置
7. **单元测试**: Vitest + jsdom 环境

## 开发规范

### 提交规范

```bash
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式调整
refactor: 重构
test: 测试相关
chore: 构建/工具配置
```

### 代码规范

- 使用 TypeScript 严格模式
- 遵循 Vue 3 Composition API 风格
- 组件采用 `<script setup>` 语法
- 使用 ESLint + Prettier（待配置）

## 后续计划

### 未完成任务

1. **基础设施**
   - ESLint + Prettier 配置
   - Git Hooks (Husky + Commitlint)
   - GitHub Actions CI/CD

2. **文档**
   - 开发指南
   - API 接口文档
   - 架构设计文档

3. **业务功能**
   - 用户管理完整 CRUD
   - 菜单管理（树形结构）
   - 字典管理完整功能
   - 操作日志查询
   - API 密钥生成与管理

4. **优化**
   - 动态路由加载（基于后端菜单配置）
   - 按钮级权限指令（`v-permission`）
   - 暗黑模式完整支持
   - 国际化 (i18n)

## 验收状态

### 阶段 1: 工程骨架搭建 ✅
- ✅ pnpm workspace 配置
- ✅ shared 包初始化
- ✅ admin-ui 应用初始化

### 阶段 2: 公共底座与拦截器 ✅
- ✅ HTTP Client 封装
- ✅ 认证模块（Token 管理、登录/登出）
- ✅ 通用工具函数库
- ✅ TypeScript 类型定义

### 阶段 3: 管理后台业务开发 ✅
- ✅ 环境变量配置
- ✅ Element Plus + Pinia + Vue Router 集成
- ✅ 路由配置与权限守卫
- ✅ Pinia 状态管理
- ✅ Layout 布局组件（Sidebar + Navbar）
- ✅ 登录页面
- ✅ 仪表盘首页
- ✅ 管理页面占位

## 构建产物

生产构建后生成 `dist` 目录：

```bash
dist/
├── index.html
├── assets/
│   ├── index-*.css      # 样式文件
│   ├── index-*.js       # JavaScript 打包文件（主包 ~1.3MB）
│   └── ...
```

## 许可证

与 DayDayUP 主项目保持一致

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

---

**当前版本**: v1.0.0  
**最后更新**: 2026-06-22  
**构建状态**: ✅ 编译通过 | ✅ 测试通过
