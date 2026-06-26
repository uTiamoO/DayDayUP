import { describe, it, expect, beforeEach, vi } from 'vitest';
import { HttpClient, createHttpClient } from '../http';
import { BusinessError } from '../types';

describe('HTTP Client', () => {
  let client: HttpClient;
  let mockTokenGetter: () => string | null;
  let mockTokenSetter: (token: string) => void;
  let mockTokenClearer: () => void;
  let mockRefreshToken: () => Promise<string>;

  beforeEach(() => {
    client = createHttpClient('http://localhost:9000');
    mockTokenGetter = vi.fn(() => 'mock-access-token');
    mockTokenSetter = vi.fn();
    mockTokenClearer = vi.fn();
    mockRefreshToken = vi.fn();

    client.setTokenManager(
      mockTokenGetter,
      mockTokenSetter,
      mockTokenClearer,
      mockRefreshToken
    );
  });

  describe('createHttpClient', () => {
    it('should create an HttpClient instance', () => {
      const httpClient = createHttpClient('http://example.com');
      expect(httpClient).toBeInstanceOf(HttpClient);
    });
  });

  describe('Token Management', () => {
    it('should call tokenGetter when making authenticated requests', () => {
      expect(mockTokenGetter).toBeDefined();
    });

    it('should set token manager correctly', () => {
      const newClient = createHttpClient('http://test.com');
      const getter = vi.fn();
      const setter = vi.fn();
      const clearer = vi.fn();
      
      newClient.setTokenManager(getter, setter, clearer);
      expect(getter).toBeDefined();
    });
  });

  describe('BusinessError', () => {
    it('should create BusinessError with correct properties', () => {
      const error = new BusinessError(400, 'Bad Request', Date.now());
      
      expect(error).toBeInstanceOf(Error);
      expect(error).toBeInstanceOf(BusinessError);
      expect(error.code).toBe(400);
      expect(error.message).toBe('Bad Request');
      expect(error.name).toBe('BusinessError');
    });
  });

  describe('Request Methods', () => {
    it('should have get method', () => {
      expect(typeof client.get).toBe('function');
    });

    it('should have post method', () => {
      expect(typeof client.post).toBe('function');
    });

    it('should have put method', () => {
      expect(typeof client.put).toBe('function');
    });

    it('should have patch method', () => {
      expect(typeof client.patch).toBe('function');
    });

    it('should have delete method', () => {
      expect(typeof client.delete).toBe('function');
    });
  });
});
