# @daydayup/shared Vitest & TypeScript Setup Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 搭建 @daydayup/shared 包的 Vitest 单元测试与 TypeScript 验证环境，并严格通过 TDD 流程实现并测试一个简单的 sum 函数。

**Architecture:** 在 `daydayup-ui/packages/shared` 包中配置 package.json 和 tsconfig.json，安装 ts 编译及 Vitest 单测工具，使用 TDD 循环（红灯-绿灯）验证环境。

**Tech Stack:** TypeScript, Vitest, pnpm

---

### Task 1: 初始化配置文件与测试框架

**Files:**
- Create: `daydayup-ui/packages/shared/package.json`
- Create: `daydayup-ui/packages/shared/tsconfig.json`

**Step 1: 创建 package.json**
在 `daydayup-ui/packages/shared/package.json` 中写入以下内容：
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

**Step 2: 创建 tsconfig.json**
在 `daydayup-ui/packages/shared/tsconfig.json` 中写入以下内容：
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

**Step 3: 安装依赖**
在项目 `daydayup-ui` 目录下执行依赖安装。
运行：`npx pnpm install`

---

### Task 2: TDD 阶段一：编写失败测试并红灯

**Files:**
- Create: `daydayup-ui/packages/shared/src/utils/__tests__/math.test.ts`
- Create: `daydayup-ui/packages/shared/src/utils/math.ts` (空文件)

**Step 1: 编写失败的测试文件**
在 `daydayup-ui/packages/shared/src/utils/__tests__/math.test.ts` 中写入：
```typescript
import { describe, it, expect } from 'vitest';
import { sum } from '../math';

describe('Math Utils', () => {
  it('should correctly sum two numbers', () => {
    expect(sum(2, 3)).toBe(5);
  });
});
```

**Step 2: 创建空的被测文件**
在 `daydayup-ui/packages/shared/src/utils/math.ts` 中创建一个空文件，以确保测试文件在 TypeScript 编译时不会因为“找不到模块 ../math”而完全阻断，但内部暂不写具体实现（或者仅声明空函数 `export function sum(...) {}`，使其返回错误的值例如 `return 0;`）。
```typescript
export function sum(a: number, b: number): number {
  return 0;
}
```

**Step 3: 运行测试并验证红灯**
在 `daydayup-ui/packages/shared` 目录下执行测试。
运行：`npx pnpm test`
预期结果：测试运行并失败（报错 `expected 0, got 5` 或者类似的断言失败，这就是预期的红灯）。

---

### Task 3: TDD 阶段二：编写最小实现并绿灯

**Files:**
- Modify: `daydayup-ui/packages/shared/src/utils/math.ts`
- Create: `daydayup-ui/packages/shared/src/index.ts`

**Step 1: 编写最小实现**
在 `daydayup-ui/packages/shared/src/utils/math.ts` 中写入正确的实现：
```typescript
export function sum(a: number, b: number): number {
  return a + b;
}
```

**Step 2: 导出模块**
在 `daydayup-ui/packages/shared/src/index.ts` 导出此方法：
```typescript
export * from './utils/math';
```

**Step 3: 运行测试验证绿灯**
在 `daydayup-ui/packages/shared` 目录下再次执行测试。
运行：`npx pnpm test`
预期结果：测试全部通过（绿灯）。

---

### Task 4: Git 暂存与提交

**Step 1: 提交代码**
执行 git add 和 git commit，提交信息为：`test(shared): 搭建 shared 包 of Vitest 单元测试与 TypeScript 验证`。
运行：
```bash
git add daydayup-ui/packages/shared
git commit -m "test(shared): 搭建 shared 包的 Vitest 单元测试与 TypeScript 验证"
```
