import { jest } from '@jest/globals';

export function resolvedMock<T>(
  value: T,
): jest.Mock<(...args: any[]) => Promise<T>> {
  return jest.fn<(...args: any[]) => Promise<T>>().mockResolvedValue(value);
}
