import { jest } from '@jest/globals';

export function leanExec<T>(value: T): {
  lean: () => { exec: jest.Mock<() => Promise<T>> };
} {
  return {
    lean: () => ({
      exec: jest.fn<() => Promise<T>>().mockResolvedValue(value),
    }),
  };
}
