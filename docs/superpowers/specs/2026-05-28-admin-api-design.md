# Admin 后端管理 API 完善设计

## 1. 背景与目标

DayDayUP 当前处于平台通信已跑通、业务管理能力待补齐的阶段。`daydayup-admin-biz` 已具备用户、角色、权限、菜单、字典和操作日志等基础表结构、Entity 与 Mapper，但对外管理接口仍以登录鉴权所需的用户查询和当前用户信息为主。

本次目标是补齐 admin 后端管理 API，为后续接入前端后台页面和其他模块管理功能提供稳定基础。

本次不包含以下内容：

- 不新增前端页面。
- 不引入多租户设计。
- 不实现角色权限分配接口。
- 不引入代码生成器。

## 2. 总体边界

本次只修改后端，重点在 `daydayup-admin-biz` 与必要的公共模块。

按现有表结构拆分为 6 个管理域：

- 用户管理
- 角色管理
- 权限管理
- 菜单管理
- 字典与字典项管理
- 操作日志查询

各管理域遵循 `Controller -> Service -> Mapper -> Entity` 分层。Controller 负责 HTTP 入参、权限注解和响应包装；Service 负责业务规则；Mapper 继续使用 MyBatis-Plus 基础能力。

## 3. 通用 API 约定

### 3.1 接口形态

除特殊说明外，管理接口遵循以下约定：

| 能力 | 方法 | 路径 |
| --- | --- | --- |
| 分页列表 | `GET` | `/xxx/page` |
| 详情 | `GET` | `/xxx/{id}` |
| 新增 | `POST` | `/xxx` |
| 修改 | `PUT` | `/xxx/{id}` |
| 状态变更 | `PATCH` | `/xxx/{id}/status` |
| 删除 | `DELETE` | `/xxx/{id}` |

删除能力不是所有模块都支持。用户、角色、权限不开放删除接口；菜单、字典和字典项开放逻辑删除；操作日志只读。

### 3.2 分页标准

新增统一分页对象，放在 `daydayup-common-core`：

- `PageQuery`
  - `pageNum`：页码，默认值为 1，最小值为 1。
  - `pageSize`：每页数量，默认值为 10，范围为 1～100。
- `PageResult<T>`
  - `records`：当前页数据。
  - `total`：总记录数。
  - `pageNum`：当前页码。
  - `pageSize`：每页数量。
  - `pages`：总页数。

Controller 对外统一返回 `R<PageResult<VO>>`。MyBatis-Plus 的分页对象只在 Service 或 Mapper 内部使用，由 Service 转换为 `PageResult<T>`。

### 3.3 DTO 入参和响应对象

每个管理域新增 DTO 入参和 VO 响应对象，避免直接暴露 Entity。接口入参必须封装为 DTO，不使用散落的基础类型参数承载业务字段。

公共请求 DTO 放在 `daydayup-common-core`，用于统一审计字段、扩展字段和后续模块复用：

- `BaseRequestDTO`：所有写操作请求 DTO 的父类，承载公共请求能力；本轮先保持轻量，不放业务字段。
- `BasePageQueryDTO`：分页查询 DTO 父类，继承或组合分页能力，承载 `pageNum`、`pageSize`。
- `BaseStatusDTO`：状态变更 DTO 父类，承载 `status`。

各管理域按用途派生具体 DTO：

- `XxxPageQueryDTO`：分页查询条件，继承 `BasePageQueryDTO`。
- `XxxCreateDTO`：新增入参，继承 `BaseRequestDTO`。
- `XxxUpdateDTO`：修改入参，继承 `BaseRequestDTO`。
- `XxxStatusDTO`：状态变更入参，可继承 `BaseStatusDTO`。
- `XxxVO`：响应对象。

修改接口以路径 `id` 为准，不信任 body 中的 `id`。DTO 父类只承载跨模块稳定字段，不把具体业务字段上提到父类，避免后续模块被迫继承无关字段。

## 4. CRUD 复用设计

为减少后续模块新增 CRUD 时的重复代码，本次只抽象 Service 层、DTO 父类和分页对象，不抽象 Controller。Controller 作为 API 契约层保持显式写法。

### 4.1 抽象边界

公共 CRUD 能力放在公共模块中，建议按依赖分层：

- `daydayup-common-core`：分页对象、基础请求 DTO 父类。
- `daydayup-common-mybatis` 或 `daydayup-common-web`：CRUD Service 抽象与模板实现。

不抽象 Controller 的原因是：各模块的 URL、权限码、请求对象和特殊规则差异较大。所有接口方法在具体 Controller 中显式声明，便于阅读 `@GetMapping`、`@PostMapping`、`@PreAuthorize`、`@Valid` 和 OpenAPI 注解，也避免删除、树形、只读等差异把 BaseController 推向复杂继承。

### 4.2 Service 抽象

设计通用接口，例如：

- `BaseCrudService<E, ID, CreateDTO, UpdateDTO, VO>`：定义分页、详情、新增、修改、状态变更等通用能力。
- `DeletableCrudService<ID>`：单独定义删除能力，仅允许删除的模块实现。
- `AbstractCrudService<M, E, ID, CreateDTO, UpdateDTO, VO>`：提供模板方法实现。

Controller 层只遵循命名和方法形态规范，不通过父类复用映射方法。重复的委托代码保留在具体 Controller 中，换取 API 契约清晰和权限注解明确。

删除能力必须拆成可选接口，用户、角色、权限不实现删除接口。

### 4.3 模板方法

通用流程由抽象类固定：

1. 参数和状态校验。
2. 调用业务校验 hook。
3. 请求对象转换为 Entity。
4. 持久化。
5. Entity 转换为 VO。

各模块通过 hook 覆盖差异点：

- `validateBeforeCreate`
- `validateBeforeUpdate`
- `validateBeforeStatusChange`
- `validateBeforeDelete`
- `toEntity`
- `updateEntity`
- `toVO`

### 4.4 策略与规约

以下规则由模块实现，不放进通用抽象硬编码：

- 用户名唯一。
- 角色 `code` 唯一。
- 权限 `code` 唯一。
- 菜单 `code` 唯一。
- 字典 `code` 唯一。
- 字典项 `(dict_code, value)` 唯一。
- 菜单删除前检查是否存在子节点。
- 用户、角色、权限禁止删除。

## 5. 各模块设计

### 5.1 用户管理 `/users`

支持能力：

- 分页列表
- 详情
- 新增
- 修改
- 启停用

关键规则：

- 新增和修改时校验 `username` 唯一。
- 新增密码使用 BCrypt 加密。
- 普通更新接口不返回密文密码，也不修改密文密码。
- 停用用户后，登录侧根据 `status` 拒绝登录。
- 不开放删除接口。

### 5.2 角色管理 `/roles`

支持能力：

- 分页列表
- 详情
- 新增
- 修改
- 启停用

关键规则：

- `code` 唯一。
- 不开放删除接口。
- 本轮不实现角色权限分配接口。

### 5.3 权限管理 `/permissions`

支持能力：

- 分页列表
- 详情
- 新增
- 修改
- 启停用

关键规则：

- `code` 唯一。
- 权限类型沿用 `MENU`、`BUTTON`、`API`。
- 不开放删除接口，避免历史角色授权悬挂。

### 5.4 菜单管理 `/menus`

支持能力：

- 列表或树
- 详情
- 新增
- 修改
- 启停用
- 逻辑删除

关键规则：

- `code` 唯一。
- 支持 `parentId=0` 作为根节点。
- 删除前检查是否存在子菜单；存在子菜单时拒绝删除。

### 5.5 字典管理 `/dicts` 与 `/dict-items`

字典类型支持能力：

- 分页列表
- 详情
- 新增
- 修改
- 启停用
- 逻辑删除

字典项支持能力：

- 按 `dictCode` 查询
- 分页列表
- 详情
- 新增
- 修改
- 启停用
- 逻辑删除

关键规则：

- 字典 `code` 唯一。
- 字典项 `(dict_code, value)` 唯一。

### 5.6 操作日志 `/oper-logs`

支持能力：

- 分页列表
- 详情

查询条件：

- 用户名
- 模块
- 成功状态
- 时间范围

操作日志不开放新增、修改和删除接口。日志写入由日志组件后续负责。

## 6. 权限设计

本轮采用细粒度权限码，接口逐个添加 `@PreAuthorize`。

权限码如下：

- 用户：`admin:user:list`、`admin:user:detail`、`admin:user:create`、`admin:user:update`、`admin:user:status`
- 角色：`admin:role:list`、`admin:role:detail`、`admin:role:create`、`admin:role:update`、`admin:role:status`
- 权限：`admin:permission:list`、`admin:permission:detail`、`admin:permission:create`、`admin:permission:update`、`admin:permission:status`
- 菜单：`admin:menu:list`、`admin:menu:detail`、`admin:menu:create`、`admin:menu:update`、`admin:menu:status`、`admin:menu:delete`
- 字典：`admin:dict:list`、`admin:dict:detail`、`admin:dict:create`、`admin:dict:update`、`admin:dict:status`、`admin:dict:delete`
- 字典项：`admin:dict-item:list`、`admin:dict-item:detail`、`admin:dict-item:create`、`admin:dict-item:update`、`admin:dict-item:status`、`admin:dict-item:delete`
- 操作日志：`admin:oper-log:list`、`admin:oper-log:detail`

同时在安全层支持通配符权限匹配：

- 用户拥有 `admin:*` 时，可以访问所有 `admin:` 前缀权限。
- 用户拥有 `game:*` 时，可以访问所有 `game:` 前缀权限。
- 普通角色仍按具体权限码授权。

种子数据保留超级管理员的 `admin:*` 权限，不为其显式绑定所有细粒度权限。细粒度权限定义仍写入 `sys_permission`，供后续角色授权使用。

## 7. 错误处理与校验

系统边界入参使用 Bean Validation，例如必填、长度和状态值范围。

业务校验放在 Service 中，例如唯一性校验、禁删规则和树形结构约束。错误处理复用 `daydayup-common-web` 全局异常机制；如现有异常类型无法表达业务错误，新增最小业务异常类型。

## 8. 实施阶段

### 阶段 1：公共基础

- 新增 `PageQuery` 与 `PageResult`。
- 新增 CRUD Service 抽象和模板实现。
- 删除能力拆成可选接口。
- 调整安全层，支持通配符权限匹配。

### 阶段 2：admin 基础管理 API

- 为用户、角色、权限、菜单、字典、字典项和操作日志新增 DTO、VO、Service 与 Controller。
- 按模块落地业务规则和权限注解。

### 阶段 3：权限种子数据

- 补齐细粒度权限定义。
- 超级管理员保留 `admin:*`。
- 普通用户不默认获得 admin 管理权限。

### 阶段 4：验证

- 编译相关 Maven 模块。
- 验证分页返回结构。
- 验证唯一性校验。
- 验证 `admin:*` 通配符权限。
- 验证用户、角色、权限不开放删除。
- 验证菜单删除前子节点检查。
- 验证操作日志只读接口。

如本地数据库或中间件不可用，至少完成可执行的编译和单元级验证，并明确未覆盖项。
