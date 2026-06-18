
import { sum } from '@daydayup/shared';

// 在控制台输出 shared 包内的 sum 函数计算结果
console.log('Math test from shared package: 1 + 2 =', sum(1, 2));

// 获取应用挂载节点
const app = document.getElementById('app');
if (app) {
  // 向页面渲染简单的内容并展示 sum 的结果，以验证依赖工作正常
  app.innerHTML = `<h1>DayDayUP Admin Portal</h1><p>Shared sum result: 1 + 2 = ${sum(1, 2)}</p>`;
}
