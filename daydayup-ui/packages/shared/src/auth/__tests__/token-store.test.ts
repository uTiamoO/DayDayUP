import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TokenStore } from '../token-store';

describe('TokenStore', () => {
  let tokenStore: TokenStore;

  beforeEach(() => {
    sessionStorage.clear();
    tokenStore = new TokenStore();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  describe('Access Token Management', () => {
    it('should set and get access token', () => {
      const token = 'test-access-token';
      tokenStore.setAccessToken(token);
      
      expect(tokenStore.getAccessToken()).toBe(token);
    });

    it('should persist access token to sessionStorage', () => {
      const token = 'test-access-token';
      tokenStore.setAccessToken(token);
      
      const newStore = new TokenStore();
      expect(newStore.getAccessToken()).toBe(token);
    });

    it('should return null when no access token is set', () => {
      expect(tokenStore.getAccessToken()).toBeNull();
    });
  });

  describe('Refresh Token Management', () => {
    it('should set and get refresh token', () => {
      const token = 'test-refresh-token';
      tokenStore.setRefreshToken(token);
      
      expect(tokenStore.getRefreshToken()).toBe(token);
    });

    it('should persist refresh token to sessionStorage', () => {
      const token = 'test-refresh-token';
      tokenStore.setRefreshToken(token);
      
      const newStore = new TokenStore();
      expect(newStore.getRefreshToken()).toBe(token);
    });
  });

  describe('Token Pairs', () => {
    it('should set both tokens at once', () => {
      const accessToken = 'access-token';
      const refreshToken = 'refresh-token';
      
      tokenStore.setTokens(accessToken, refreshToken);
      
      expect(tokenStore.getAccessToken()).toBe(accessToken);
      expect(tokenStore.getRefreshToken()).toBe(refreshToken);
    });
  });

  describe('Clear Tokens', () => {
    it('should clear all tokens', () => {
      tokenStore.setTokens('access', 'refresh');
      tokenStore.clearTokens();
      
      expect(tokenStore.getAccessToken()).toBeNull();
      expect(tokenStore.getRefreshToken()).toBeNull();
    });

    it('should remove tokens from sessionStorage', () => {
      tokenStore.setTokens('access', 'refresh');
      tokenStore.clearTokens();
      
      const newStore = new TokenStore();
      expect(newStore.getAccessToken()).toBeNull();
      expect(newStore.getRefreshToken()).toBeNull();
    });
  });

  describe('Authentication Status', () => {
    it('should return false when not authenticated', () => {
      expect(tokenStore.isAuthenticated()).toBe(false);
    });

    it('should return true when access token exists', () => {
      tokenStore.setAccessToken('test-token');
      expect(tokenStore.isAuthenticated()).toBe(true);
    });

    it('should return false after clearing tokens', () => {
      tokenStore.setAccessToken('test-token');
      tokenStore.clearTokens();
      expect(tokenStore.isAuthenticated()).toBe(false);
    });
  });
});
