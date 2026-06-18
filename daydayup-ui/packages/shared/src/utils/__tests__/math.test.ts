import { describe, it, expect } from 'vitest';
import { sum } from '../math';

describe('Math Utils', () => {
  // 测试：应当正确计算两个数字的和
  it('should correctly sum two numbers', () => {
    expect(sum(2, 3)).toBe(5);
  });
});
