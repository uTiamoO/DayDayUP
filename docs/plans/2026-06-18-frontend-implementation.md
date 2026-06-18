# DayDayUP Frontend Monorepo Setup Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 初始化前端 Monorepo 工程骨架，构建包含共享包 `shared` 与管理后台子应用 `admin-ui` 的 pnpm workspace 环境，并配齐 TypeScript 和 Vitest 测试环境以支持后续的 TDD 开发。

**Architecture:** 采用 pnpm workspace 组织代码。`packages/shared` 作为基础工具包，提供不依赖 UI 的逻辑服务并配有 Vitest 测试；`packages/apps/admin-ui` 作为 Vue 3 业务系统，使用 Vite 作为构建工具，并通过 workspace 协议链接 `@daydayup/shared`。

**Tech Stack:** pnpm workspace, Node 20+, Vue 3, Vite, TypeScript, Vitest

---

### Task 1: 初始化工作区根目录

**Files:**
- Create: `daydayup-ui/package.json`
- Create: `daydayup-ui/pnpm-workspace.yaml`

**Step 1: 创建工作区配置文件**
新建文件 `daydayup-ui/pnpm-workspace.yaml`，内容如下：
```yaml
packages:
  - 'packages/*'
  - 'packages/apps/*'
```

**Step 2: 创建根目录 package.json**
新建文件 `daydayup-ui/package.json`，内容如下：
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

**Step 3: 运行验证**
在 `daydayup-ui` 目录下执行命令：
`pnpm install`
确保 pnpm 能够成功解析根目录的 workspace 配置，无报错。

**Step 4: Commit**
```bash
git add daydayup-ui/package.json daydayup-ui/pnpm-workspace.yaml
git commit -m "chore(ui): 初始化 pnpm workspace 根配置文件"
```

---

### Task 2: 搭建并配置共享包 `shared` 的 TDD 环境

**Files:**
- Create: `daydayup-ui/packages/shared/package.json`
- Create: `daydayup-ui/packages/shared/tsconfig.json`
- Create: `daydayup-ui/packages/shared/src/utils/math.ts`
- Create: `daydayup-ui/packages/shared/src/utils/__tests__/math.test.ts`
- Create: `daydayup-ui/packages/shared/src/index.ts`

**Step 1: 创建共享包 package.json**
配置 `daydayup-ui/packages/shared/package.json` 引入 `typescript` 和 `vitest`：
```json
{
  "name": "@daydayup/shared",
  "version": "1.0.0",
  "private": true,
  "main": "./src/index.ts",
  "types": "./src/index.ts",
  "scripts": {
    "build": "tsc --noEmit",
    "test": "vitest run"
  },
  "devDependencies": {
    "typescript": "^5.3.3",
    "vitest": "^1.3.1"
  }
}
```

**Step 2: 创建 TypeScript 配置文件**
配置 `daydayup-ui/packages/shared/tsconfig.json`：
```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "esModuleInterop": true,
    "strict": true,
    "declaration": true,
    "skipLibCheck": true
  },
  "include": ["src/**/*"]
}
```

**Step 3: 编写失败的单元测试 (TDD)**
在 `daydayup-ui/packages/shared/src/utils/__tests__/math.test.ts` 中编写测试：
```typescript
import { describe, it, expect } from 'vitest';
import { sum } from '../math';

describe('Math Utils', () => {
  it('should correctly sum two numbers', () => {
    expect(sum(2, 3)).toBe(5);
  });
});
```

**Step 4: 运行测试并确保其失败**
创建空的 `daydayup-ui/packages/shared/src/utils/math.ts` 以解决编译报错，随后在 `daydayup-ui/packages/shared` 目录下运行：
`pnpm test`
预期输出：编译失败或测试失败 (找不到 `sum` 函数)。

**Step 5: 编写最小实现**
在 `daydayup-ui/packages/shared/src/utils/math.ts` 中写入实现：
```typescript
export function sum(a: number, b: number): number {
  return a + b;
}
```
并在 `daydayup-ui/packages/shared/src/index.ts` 中将其导出：
```typescript
export * from './utils/math';
```

**Step 6: 重新运行测试并确保其通过**
在 `daydayup-ui/packages/shared` 目录下运行：
`pnpm test`
预期输出：测试全部通过 (PASS)。

**Step 7: Commit**
```bash
git add daydayup-ui/packages/shared
git commit -m "test(shared): 搭建 shared 包的 Vitest 单元测试与 TypeScript 验证"
```

---

### Task 3: 创建并配置管理后台子应用 `admin-ui`

**Files:**
- Create: `daydayup-ui/packages/apps/admin-ui/package.json`
- Create: `daydayup-ui/packages/apps/admin-ui/vite.config.ts`
- Create: `daydayup-ui/packages/apps/admin-ui/tsconfig.json`
- Create: `daydayup-ui/packages/apps/admin-ui/index.html`
- Create: `daydayup-ui/packages/apps/admin-ui/src/main.ts`

**Step 1: 创建应用 package.json 并依赖 `shared`**
配置 `daydayup-ui/packages/apps/admin-ui/package.json`：
```json
{
  "name": "@daydayup/admin-ui",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "@daydayup/shared": "workspace:*",
    "vue": "^3.4.15"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.3",
    "typescript": "^5.3.3",
    "vite": "^5.0.12",
    "vue-tsc": "^1.8.27"
  }
}
```

**Step 2: 创建 Vite 配置文件**
配置 `daydayup-ui/packages/apps/admin-ui/vite.config.ts`：
```typescript
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  }
});
```

**Step 3: 创建 TypeScript 配置文件**
配置 `daydayup-ui/packages/apps/admin-ui/tsconfig.json`：
```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "preserve",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "skipLibCheck": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["src/**/*.ts", "src/**/*.d.ts", "src/**/*.tsx", "src/**/*.vue"]
}
```

**Step 4: 创建入口 HTML 与主程序**
创建 `daydayup-ui/packages/apps/admin-ui/index.html`：
```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>DayDayUP Admin Portal</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.ts"></script>
  </body>
</html>
```

创建极简的入口逻辑，并且测试引用 `@daydayup/shared` 的 `sum` 函数。
在 `daydayup-ui/packages/apps/admin-ui/src/main.ts` 写入：
```typescript
import { createApp } from 'vue';
import { sum } from '@daydayup/shared';

console.log('Math test from shared package: 1 + 2 =', sum(1, 2));

const app = document.getElementById('app');
if (app) {
  app.innerHTML = `<h1>DayDayUP Admin Portal</h1><p>Shared sum result: 1 + 2 = ${sum(1, 2)}</p>`;
}
```

**Step 5: 验证 workspace 编译与构建**
在根目录 `daydayup-ui` 下执行：
`pnpm install`
随后运行编译：
`pnpm build:admin`
预期输出：`admin-ui` 构建顺利完成，无 TypeScript 编译或解析错误。

**Step 6: Commit**
```bash
git add daydayup-ui/packages/apps/admin-ui
git commit -m "feat(admin-ui): 初始化 admin-ui 应用并引入 shared 依赖"
```
