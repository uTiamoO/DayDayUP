import { describe, it, expect } from 'vitest';
import { sum } from '../math';

describe('Math Utils', () => {
  it('should correctly sum two numbers', () => {
    expect(sum(2, 3)).toBe(5);
  });
});
