/**
 * localStorage/sessionStorage 封装
 * 支持 JSON 序列化和反序列化
 */

export class StorageHelper {
  private storage: Storage;

  constructor(storage: Storage) {
    this.storage = storage;
  }

  /**
   * 设置存储项
   */
  setItem<T>(key: string, value: T): void {
    try {
      const serialized = JSON.stringify(value);
      this.storage.setItem(key, serialized);
    } catch (error) {
      console.error(`Failed to set item ${key}:`, error);
    }
  }

  /**
   * 获取存储项
   */
  getItem<T>(key: string): T | null {
    try {
      const item = this.storage.getItem(key);
      if (item === null) return null;
      return JSON.parse(item) as T;
    } catch (error) {
      console.error(`Failed to get item ${key}:`, error);
      return null;
    }
  }

  /**
   * 移除存储项
   */
  removeItem(key: string): void {
    try {
      this.storage.removeItem(key);
    } catch (error) {
      console.error(`Failed to remove item ${key}:`, error);
    }
  }

  /**
   * 清空所有存储
   */
  clear(): void {
    try {
      this.storage.clear();
    } catch (error) {
      console.error('Failed to clear storage:', error);
    }
  }

  /**
   * 检查是否存在某个键
   */
  hasItem(key: string): boolean {
    return this.storage.getItem(key) !== null;
  }
}

export const localStorageHelper = new StorageHelper(localStorage);
export const sessionStorageHelper = new StorageHelper(sessionStorage);
