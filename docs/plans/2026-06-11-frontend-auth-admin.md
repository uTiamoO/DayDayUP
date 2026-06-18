# Frontend Auth Admin Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 新建 DayDayUP 前端工程，接入当前 `auth` 与 `admin` 接口，先支撑管理后台登录、鉴权、用户管理、菜单管理、字典管理、操作日志和 API Key 管理。

**Architecture:** 前端独立为 `daydayup-ui` 子工程，通过 gateway 调用业务 API，通过 Spring Authorization Server 完成 OAuth2 Authorization Code + PKCE 登录。所有后端响应统一在 HTTP 层解包 `R<T>`，所有分页统一适配 `PageResult<T>`。

**Tech Stack:** Vue 3、Vite、TypeScript、Vue Router、Pinia、Element Plus、Axios、Vitest、Playwright、ESLint、Prettier。

---

## 1. 当前后端接口判断

### 1.1 服务与网关

| 服务 | 本地端口 | 前端访问方式 | 说明 |
| --- | ---: | --- | --- |
| gateway | `9000` | `http://127.0.0.1:9000` | 前端业务 API 统一入口。 |
| auth | `9200` | `http://127.0.0.1:9200` | OAuth2/OIDC 认证入口，当前不建议经 gateway 发起 authorize。 |
| admin-biz | `9100` | 经 gateway `/admin/**` | 网关 `StripPrefix=1` 后转发到真实 admin 路径。 |

网关路由：

| 前端路径 | 后端真实路径 | 说明 |
| --- | --- | --- |
| `/auth/**` | auth `/**` | token、JWK、OIDC discovery 可经 gateway。 |
| `/admin/**` | admin-biz `/**` | 管理后台业务 API。 |

### 1.2 统一响应

后端业务接口返回：

```ts
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}
```

成功码为 `200`。分页返回：

```ts
export interface PageResult<T> {
  records: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
}
```

前端必须在 `src/shared/api/http.ts` 统一解包，不允许页面直接判断 `code`。

### 1.3 认证现状

`auth` 已接入 Spring Authorization Server：

- 种子客户端：`admin-web`
- 客户端类型：public client
- 授权方式：`authorization_code`、`refresh_token`
- PKCE：已开启
- redirect URI：`http://localhost:5173/callback`
- scope：`openid profile admin:*`
- access token TTL：`7200` 秒
- refresh token TTL：`604800` 秒

当前 `admin-web` 不支持 `password` grant。前端正式方案必须使用 Authorization Code + PKCE，不要在 SPA 长期收集用户名密码。

### 1.4 当前可直接适配的 Admin API

前端经 gateway 调用，统一加 `/admin` 前缀。

| 模块 | 接口 |
| --- | --- |
| 当前用户 | `GET /admin/me` |
| 用户管理 | `GET /admin/users/manage/page`、`GET /admin/users/manage/{id}`、`POST /admin/users/manage`、`PUT /admin/users/manage/{id}`、`PATCH /admin/users/manage/{id}/status` |
| 菜单管理 | `GET /admin/menus/page`、`GET /admin/menus/{id}`、`POST /admin/menus`、`PUT /admin/menus/{id}`、`PATCH /admin/menus/{id}/status`、`DELETE /admin/menus/{id}` |
| 字典管理 | `GET /admin/dicts/page`、`GET /admin/dicts/{id}`、`POST /admin/dicts`、`PUT /admin/dicts/{id}`、`PATCH /admin/dicts/{id}/status`、`DELETE /admin/dicts/{id}` |
| 字典项管理 | `GET /admin/dict-items/page`、`GET /admin/dict-items/{id}`、`POST /admin/dict-items`、`PUT /admin/dict-items/{id}`、`PATCH /admin/dict-items/{id}/status`、`DELETE /admin/dict-items/{id}` |
| 操作日志 | `GET /admin/oper-logs/page`、`GET /admin/oper-logs/{id}` |
| API Key | `POST /admin/api-keys`、`DELETE /admin/api-keys/{apiKey}` |

### 1.5 当前不应硬做的能力

| 能力 | 当前状态 | 前端处理 |
| --- | --- | --- |
| 角色 CRUD | 仅 `auth /api/roles/by-ids`，无 admin CRUD Controller | 预留类型和路由，不上线页面。 |
| 权限 CRUD | 有 DTO/VO 痕迹，无 Controller | 预留模块，不上线页面。 |
| 菜单树 | 当前只有 `/menus/page` | 一期前端用分页列表；需要树形菜单时补 `GET /menus/tree`。 |
| 当前用户通用接口 | `/admin/me` 需要 `admin:*` | 一期面向超级管理员；后续后端放宽到任意登录用户。 |
| 服务端登出 | 代码有黑名单服务，但未见前端可用 logout Controller | 一期本地清 token；正式上线前补 token revocation/logout。 |

## 2. 推荐前端工程方案

### 2.1 推荐方案：Vue 3 + Element Plus

推荐使用 Vue 3 + Vite + TypeScript + Element Plus。这个选择贴合国内后台系统常见技术栈，表格、表单、弹窗、权限按钮、分页器都能少造轮子。

备选方案：

| 方案 | 优点 | 缺点 | 结论 |
| --- | --- | --- | --- |
| Vue 3 + Element Plus | 管理端组件成熟，开发快，国内团队接手成本低 | 默认视觉普通，需要约束样式 | 推荐。 |
| React + Ant Design | 工程化强，生态完整 | 当前仓库无前端基线，团队若偏 Vue 会增加沟通成本 | 可用但不首选。 |
| Ant Design Pro / RuoYi Vue | 后台模板完整 | 模板包袱重，容易带入不需要的权限、路由和样式约定 | 不建议一上来套。 |

### 2.2 工程位置

新建独立子工程：

```text
daydayup-ui/
  package.json
  vite.config.ts
  src/
```

不建议塞进 Java 模块里。前后端生命周期不同，放在根目录同级更清楚。

### 2.3 目录结构

```text
daydayup-ui/src/
  app/
    App.vue
    router.ts
    bootstrap.ts
  assets/
  layouts/
    AdminLayout.vue
    AuthLayout.vue
  pages/
    auth/
      LoginRedirectPage.vue
      OAuthCallbackPage.vue
    admin/
      DashboardPage.vue
      UserPage.vue
      MenuPage.vue
      DictPage.vue
      DictItemPage.vue
      OperLogPage.vue
      ApiKeyPage.vue
  shared/
    api/
      http.ts
      types.ts
    auth/
      oauth.ts
      pkce.ts
      token-store.ts
    permissions/
      can.ts
    ui/
      PageTable.vue
      SearchForm.vue
      StatusTag.vue
  modules/
    users/
      api.ts
      types.ts
    menus/
      api.ts
      types.ts
    dicts/
      api.ts
      types.ts
    oper-logs/
      api.ts
      types.ts
    api-keys/
      api.ts
      types.ts
  stores/
    auth.ts
    user.ts
```

## 3. 认证与权限设计

> **更新（2026-06-18）**：管理后台登录已改为 **DirectToken 直发**，不再走授权码 + PKCE。
> 前端 `POST /auth/login`（网关改写到 auth `/api/login`）提交账号密码直接换 `access_token` / `refresh_token`，刷新走 `POST /auth/refresh`。
> 下面 §3.2 的 PKCE 流程仅作为将来三方 / SSO 接入的备用方案保留，一期前端不实现。
> RBAC 管理统一走 `/admin/**` 门面（auth 的 `/api/{users,roles,permissions}` 已不经网关对外暴露）。

### 3.1 环境变量

创建 `daydayup-ui/.env.development`：

```env
VITE_GATEWAY_BASE_URL=http://127.0.0.1:9000
VITE_AUTH_BASE_URL=http://127.0.0.1:9200
VITE_OAUTH_CLIENT_ID=admin-web
VITE_OAUTH_REDIRECT_URI=http://localhost:5173/callback
VITE_OAUTH_SCOPE=openid profile admin:*
```

说明：

- 业务 API 走 `VITE_GATEWAY_BASE_URL`。
- OAuth authorize/login 先直连 `VITE_AUTH_BASE_URL`。当前 auth 未把 `/login` 配成 gateway 前缀，经 gateway 发起 authorize 容易重定向到错误的 `/login`。
- 生产环境若要统一经 gateway 暴露 auth，需要后端同步调整 `DAYDAYUP_AUTH_ISSUER`、gateway permit paths 和 auth 登录页路径。

### 3.2 PKCE 登录流程

1. 用户访问受保护路由。
2. 前端生成 `code_verifier`、`code_challenge`、`state`。
3. 跳转到：

```text
{AUTH_BASE_URL}/oauth2/authorize
  ?response_type=code
  &client_id=admin-web
  &redirect_uri=http://localhost:5173/callback
  &scope=openid%20profile%20admin:*
  &code_challenge={challenge}
  &code_challenge_method=S256
  &state={state}
```

4. auth 服务展示 `/login` 表单并完成登录。
5. 浏览器回到 `/callback?code=...&state=...`。
6. 前端调用 `{AUTH_BASE_URL}/oauth2/token`，用 `authorization_code + code_verifier` 换 token。
7. token 写入 `token-store`，路由进入 admin 首页。

### 3.3 Token 存储策略

一期采用：

- access token：内存 + `sessionStorage`
- refresh token：`sessionStorage`
- 默认不使用 `localStorage` 持久化 refresh token
- 关闭浏览器后需要重新登录

这不是绝对安全方案，但比把 refresh token 长期放 `localStorage` 更稳。后续如果要「记住登录」，应优先评估 BFF 或 HttpOnly Cookie。

### 3.4 请求鉴权

`src/shared/api/http.ts`：

- 请求拦截器加 `Authorization: Bearer ${accessToken}`。
- 响应拦截器统一解包 `R<T>`。
- 遇到 `401` 或业务码 `20001/20002/20006` 时尝试 refresh。
- refresh 失败后清理 token，跳转登录。
- 遇到 `403` 显示无权限页面或轻提示。

### 3.5 权限判断

权限来源：

- 优先从 `/admin/me` 的 `authorities` 读取。
- 当前 `/admin/me` 只允许 `admin:*`，一期按超级管理员后台处理。
- 后续后端放开 `/admin/me` 后，前端再启用细粒度按钮权限。

前端权限判断规则：

```ts
export function can(authorities: string[], permission: string): boolean {
  if (authorities.includes(permission)) return true;
  const namespace = permission.split(':')[0];
  return authorities.includes(`${namespace}:*`);
}
```

## 4. 页面规划

### 4.1 一期页面

| 路由 | 页面 | 权限 | 接口 |
| --- | --- | --- | --- |
| `/` | 重定向 `/admin` | 登录 | 无 |
| `/callback` | OAuth 回调 | 公开 | `/oauth2/token` |
| `/admin` | 首页 | `admin:*` | `/admin/me` |
| `/admin/users` | 用户管理 | `admin:user:list` 或 `admin:*` | `/admin/users/manage/**` |
| `/admin/menus` | 菜单管理 | `admin:menu:list` 或 `admin:*` | `/admin/menus/**` |
| `/admin/dicts` | 字典管理 | `admin:dict:list` 或 `admin:*` | `/admin/dicts/**` |
| `/admin/dict-items` | 字典项管理 | `admin:dict-item:list` 或 `admin:*` | `/admin/dict-items/**` |
| `/admin/oper-logs` | 操作日志 | `admin:oper-log:list` 或 `admin:*` | `/admin/oper-logs/**` |
| `/admin/api-keys` | API Key 管理 | `admin:apikey:create/revoke` 或 `admin:*` | `/admin/api-keys/**` |

### 4.2 二期页面

| 页面 | 后端前置条件 |
| --- | --- |
| 角色管理 | 补齐 `GET/POST/PUT/PATCH /roles/**` 或迁移到 auth 后由 admin 门面透出。 |
| 权限管理 | 补齐 `GET/POST/PUT/PATCH /permissions/**`。 |
| 角色授权 | 补齐角色权限分配接口。 |
| 用户重置密码 | 补齐 `PATCH /users/manage/{id}/password` 或 auth 门面接口。 |
| 动态路由菜单 | 补齐当前用户菜单接口，如 `GET /admin/me/menus`。 |

## 5. API 模块类型

### 5.1 用户

```ts
export interface UserDetail {
  id: number;
  username: string;
  nickname?: string;
  email?: string;
  mobile?: string;
  status: number;
  lastLoginAt?: string;
  lastLoginIp?: string;
  roleIds?: number[];
  roleCodes?: string[];
}

export interface UserQuery {
  pageNum: number;
  pageSize: number;
  username?: string;
  status?: number;
}
```

注意：当前 admin 门面引用的是 `auth-api` 的 `UserDetailVO`，字段以 `auth-api` 为准，不要用 admin-biz 旧 VO 生成类型。

### 5.2 菜单

```ts
export interface MenuItem {
  id: number;
  parentId: number;
  code: string;
  name: string;
  path?: string;
  component?: string;
  icon?: string;
  type?: string;
  permissionCode?: string;
  sort: number;
  visible: number;
  status: number;
  createTime?: string;
  updateTime?: string;
}
```

一期使用表格展示。树形菜单等后端补 `tree` 接口后再做。

### 5.3 字典与字典项

```ts
export interface Dict {
  id: number;
  code: string;
  name: string;
  status: number;
  remark?: string;
}

export interface DictItem {
  id: number;
  dictCode: string;
  value: string;
  label: string;
  sort: number;
  status: number;
  remark?: string;
}
```

### 5.4 操作日志

```ts
export interface OperLog {
  id: number;
  userId?: number;
  username?: string;
  module?: string;
  operation?: string;
  requestUrl?: string;
  requestMethod?: string;
  requestIp?: string;
  success: number;
  errorMsg?: string;
  costMs?: number;
  operTime?: string;
}
```

## 6. 实施任务

### Task 1: 创建前端工程骨架

**Files:**

- Create: `daydayup-ui/package.json`
- Create: `daydayup-ui/vite.config.ts`
- Create: `daydayup-ui/tsconfig.json`
- Create: `daydayup-ui/src/main.ts`
- Create: `daydayup-ui/src/app/App.vue`

**Step 1: 初始化工程**

Run:

```bash
pnpm create vite daydayup-ui --template vue-ts
cd daydayup-ui
pnpm add vue-router pinia axios element-plus @element-plus/icons-vue
pnpm add -D vitest @vue/test-utils jsdom eslint prettier typescript vue-tsc playwright
```

Expected: `daydayup-ui` 目录生成，依赖安装成功。

**Step 2: 固定开发端口**

`vite.config.ts` 配置：

```ts
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
  },
});
```

**Step 3: 验证**

Run:

```bash
pnpm dev
```

Expected: `http://127.0.0.1:5173` 可访问。

### Task 2: 建立 HTTP Client

**Files:**

- Create: `daydayup-ui/src/shared/api/types.ts`
- Create: `daydayup-ui/src/shared/api/http.ts`

**Step 1: 写类型**

```ts
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
}
```

**Step 2: 写 Axios 实例**

要求：

- `baseURL = import.meta.env.VITE_GATEWAY_BASE_URL`
- 统一添加 Bearer token
- `code !== 200` 时抛业务错误
- token 过期时触发 refresh

**Step 3: 单元测试**

Create: `daydayup-ui/src/shared/api/http.spec.ts`

覆盖：

- `R.ok(data)` 被解包成 `data`
- `code !== 200` 抛出包含 `message` 的异常
- `401` 会清理登录态

Run:

```bash
pnpm vitest run src/shared/api/http.spec.ts
```

### Task 3: 实现 OAuth2 PKCE

**Files:**

- Create: `daydayup-ui/src/shared/auth/pkce.ts`
- Create: `daydayup-ui/src/shared/auth/oauth.ts`
- Create: `daydayup-ui/src/shared/auth/token-store.ts`
- Create: `daydayup-ui/src/pages/auth/OAuthCallbackPage.vue`

**Step 1: PKCE 工具**

实现：

- `createCodeVerifier()`
- `createCodeChallenge(verifier)`
- `createState()`

**Step 2: 登录跳转**

`oauth.ts` 提供：

- `redirectToLogin()`
- `exchangeCodeForToken(code, state)`
- `refreshToken()`
- `logoutLocal()`

**Step 3: 回调页**

`OAuthCallbackPage.vue`：

- 读取 `code` 和 `state`
- 校验 `state`
- 调用 `exchangeCodeForToken`
- 成功后跳回原目标路由或 `/admin`

**Step 4: 验证**

Run:

```bash
pnpm vitest run src/shared/auth
```

Expected: PKCE 生成、state 校验、token 存储逻辑通过。

### Task 4: 路由与布局

**Files:**

- Create: `daydayup-ui/src/app/router.ts`
- Create: `daydayup-ui/src/layouts/AdminLayout.vue`
- Create: `daydayup-ui/src/stores/auth.ts`
- Create: `daydayup-ui/src/stores/user.ts`

**Step 1: 定义路由**

静态路由：

- `/callback`
- `/admin`
- `/admin/users`
- `/admin/menus`
- `/admin/dicts`
- `/admin/dict-items`
- `/admin/oper-logs`
- `/admin/api-keys`

**Step 2: 路由守卫**

规则：

- 未登录访问 `/admin/**`：调用 `redirectToLogin()`。
- 已登录访问 `/callback`：正常完成 token exchange。
- 权限不足：进入 `403` 页面。

**Step 3: 当前用户加载**

登录后调用 `GET /admin/me`，存储：

- `userId`
- `username`
- `authorities`

### Task 5: Admin API 模块

**Files:**

- Create: `daydayup-ui/src/modules/users/api.ts`
- Create: `daydayup-ui/src/modules/menus/api.ts`
- Create: `daydayup-ui/src/modules/dicts/api.ts`
- Create: `daydayup-ui/src/modules/oper-logs/api.ts`
- Create: `daydayup-ui/src/modules/api-keys/api.ts`

**Step 1: 用户 API**

```ts
export const userApi = {
  page: (params: UserQuery) => http.get<PageResult<UserDetail>>('/admin/users/manage/page', { params }),
  detail: (id: number) => http.get<UserDetail>(`/admin/users/manage/${id}`),
  create: (data: UserCreateInput) => http.post<UserDetail>('/admin/users/manage', data),
  update: (id: number, data: UserUpdateInput) => http.put<UserDetail>(`/admin/users/manage/${id}`, data),
  changeStatus: (id: number, status: number) => http.patch<void>(`/admin/users/manage/${id}/status`, { status }),
};
```

**Step 2: 通用 CRUD 约定**

菜单、字典、字典项复用同一形态：

- `page(params)`
- `detail(id)`
- `create(data)`
- `update(id, data)`
- `changeStatus(id, status)`
- `delete(id)`

操作日志只读，API Key 只有创建和吊销。

### Task 6: 页面实现

**Files:**

- Create: `daydayup-ui/src/pages/admin/UserPage.vue`
- Create: `daydayup-ui/src/pages/admin/MenuPage.vue`
- Create: `daydayup-ui/src/pages/admin/DictPage.vue`
- Create: `daydayup-ui/src/pages/admin/DictItemPage.vue`
- Create: `daydayup-ui/src/pages/admin/OperLogPage.vue`
- Create: `daydayup-ui/src/pages/admin/ApiKeyPage.vue`
- Create: `daydayup-ui/src/shared/ui/PageTable.vue`
- Create: `daydayup-ui/src/shared/ui/SearchForm.vue`
- Create: `daydayup-ui/src/shared/ui/StatusTag.vue`

**Step 1: 用户管理**

功能：

- 搜索：用户名、状态
- 表格：用户名、昵称、手机、邮箱、状态、最近登录时间、角色
- 操作：新增、编辑、启停用

**Step 2: 菜单管理**

功能：

- 搜索：名称、类型、状态
- 表格：名称、路径、组件、权限码、排序、可见、状态
- 操作：新增、编辑、启停用、删除

**Step 3: 字典管理**

功能：

- 字典列表：编码、名称、状态
- 字典项列表：按 `dictCode` 过滤
- 操作：新增、编辑、启停用、删除

**Step 4: 操作日志**

功能：

- 搜索：用户名、模块、成功状态、时间范围
- 表格：操作、方法、URL、IP、耗时、结果、时间
- 详情弹窗展示请求参数和响应内容

**Step 5: API Key**

功能：

- 创建 API Key 后只展示一次
- 支持复制
- 支持吊销

### Task 7: 后端配套任务

这些不是前端阻塞项，但会影响完整后台体验。

**Files:**

- Modify: `daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/.../CurrentUserController.java`
- Create/Modify: role、permission 管理 Controller
- Create/Modify: menu tree 和当前用户菜单接口
- Create/Modify: auth logout/revoke 接口

**Step 1: 放宽 `/admin/me`**

当前 `@PreAuthorize("hasAuthority('admin:*')")` 只允许超级管理员。建议改为任意登录用户可访问，返回当前用户基本信息与权限码。

**Step 2: 补角色与权限 CRUD**

补齐：

- `GET /admin/roles/page`
- `GET /admin/roles/{id}`
- `POST /admin/roles`
- `PUT /admin/roles/{id}`
- `PATCH /admin/roles/{id}/status`
- `GET /admin/permissions/page`
- `GET /admin/permissions/{id}`
- `POST /admin/permissions`
- `PUT /admin/permissions/{id}`
- `PATCH /admin/permissions/{id}/status`

**Step 3: 补菜单树**

建议新增：

- `GET /admin/menus/tree`
- `GET /admin/me/menus`

**Step 4: 补服务端登出**

建议新增适合 public client 的登出接口，至少完成 access token JTI 黑名单写入。前端本地删 token 只能让当前浏览器退出，不能让已签发 access token 立即失效。

### Task 8: 验证

**本地联调前置：**

- Redis、MySQL、Nacos 正常运行。
- gateway `9000`、auth `9200`、admin-biz `9100` 已启动。
- `admin-web` redirect URI 与 Vite 地址一致：`http://localhost:5173/callback`。

**命令：**

```bash
cd daydayup-ui
pnpm typecheck
pnpm vitest run
pnpm dev
```

**手工验收：**

1. 访问 `http://localhost:5173/admin`，跳转 auth 登录。
2. 登录成功后回到 `/callback`，再进入 `/admin`。
3. `GET /admin/me` 成功返回当前用户。
4. 用户、菜单、字典、字典项页面能分页查询。
5. 新增、编辑、启停用操作能正常提示成功或业务错误。
6. token 过期后 refresh token 能自动刷新。
7. refresh 失败后回到登录流程。

## 7. 阶段拆分

| 阶段 | 目标 | 交付 |
| --- | --- | --- |
| Phase 0 | 前端骨架 | Vite 工程、路由、布局、HTTP Client、环境变量。 |
| Phase 1 | 登录闭环 | PKCE 登录、callback、token refresh、`/admin/me`。 |
| Phase 2 | Admin 基础管理 | 用户、菜单、字典、字典项、日志、API Key。 |
| Phase 3 | 权限增强 | 按钮权限、403、动态菜单、角色权限页面。 |
| Phase 4 | 工程质量 | OpenAPI 类型生成、E2E、CI、构建部署。 |

## 8. 风险与建议

| 风险 | 影响 | 建议 |
| --- | --- | --- |
| auth authorize 经 gateway 可能跳错 `/login` | 登录流程不稳定 | 一期直连 auth `9200`；生产再统一 issuer 与 gateway 路径。 |
| `/admin/me` 当前仅 `admin:*` | 非超管无法进入后台 | 后端改为任意登录用户可访问，再由权限控制菜单。 |
| 角色/权限接口未暴露 | RBAC 页面不能上线 | 前端预留模块，等后端补接口。 |
| 缺少服务端登出 | token 不能立即失效 | 补 logout/revoke 端点，写入黑名单。 |
| 手写类型容易漂移 | 后端字段变化前端不感知 | Phase 4 接入 OpenAPI 类型生成。 |

## 9. 推荐落地顺序

1. 先搭 `daydayup-ui`，固定 Vite 端口 `5173`。
2. 先打通 PKCE 登录和 `/admin/me`，别急着写一堆页面。
3. 写统一 `http.ts`，把 `R<T>` 和 `PageResult<T>` 封死。
4. 做用户管理页，验证表格、表单、状态切换的通用模式。
5. 复用模式做菜单、字典、字典项、日志、API Key。
6. 后端补齐 `/admin/me`、角色、权限、菜单树、登出后，再做动态权限后台。
