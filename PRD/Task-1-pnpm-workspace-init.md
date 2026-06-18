# pnpm workspace 初始化实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在项目根目录下创建并初始化 `daydayup-ui` 前端 monorepo 工作区的基础配置文件，验证 `pnpm install` 并提交至 Git。

**Architecture:** 创建包含 `packages/*` 和 `packages/apps/*` 的 pnpm monorepo 结构，并在根目录下提供统一的开发、构建、测试脚本以便于统一调度子模块。

**Tech Stack:** pnpm Workspace, Node.js >= 20.0.0, Git

---

### Task 1: 创建 daydayup-ui 目录并初始化根配置文件

**Files:**
- Create: `daydayup-ui/pnpm-workspace.yaml`
- Create: `daydayup-ui/package.json`

**Step 1: 创建 daydayup-ui 目录**
在项目根目录 `D:/ideaWorkspace/github/DayDayUP` 下创建 `daydayup-ui` 文件夹。

**Step 2: 创建并写入 daydayup-ui/pnpm-workspace.yaml**
配置 pnpm 成员目录：
```yaml
packages:
  - 'packages/*'
  - 'packages/apps/*'
```

**Step 3: 创建并写入 daydayup-ui/package.json**
配置 monorepo 根 package.json：
```json
{
  "name": "daydayup-ui",
  "version": "1.0.0",
  "private": true,
  "description": "DayDayUP Frontend Monorepo",
  "scripts": {
    "dev:admin": "pnpm --filter @daydayup/admin-ui dev",
    "build:admin": "pnpm --filter @daydayup/admin-ui build",
    "test:shared": "pnpm --filter @daydayup/shared test",
    "build:shared": "pnpm --filter @daydayup/shared build"
  },
  "engines": {
    "node": ">=20.0.0"
  }
}
```

**Step 4: 运行 pnpm install 并验证**
在 `daydayup-ui` 目录下执行 `pnpm install`。
运行命令：
```powershell
cd daydayup-ui
pnpm install
```
预期结果：命令成功执行，解析无报错，生成 `pnpm-lock.yaml`（如果当前无子依赖包，会生成基本的 lockfile 或由于没有 packages 报错，注意如果没有 packages 会提示零依赖，但应能成功运行）。

**Step 5: Git 暂存并提交**
将新建的 `pnpm-workspace.yaml` 和 `package.json` 添加到 Git 暂存区并提交。
运行命令：
```powershell
git add daydayup-ui/pnpm-workspace.yaml daydayup-ui/package.json
git commit -m "chore(ui): 初始化 pnpm workspace 根配置文件"
```
预期结果：Git 成功提交 2 个文件。
