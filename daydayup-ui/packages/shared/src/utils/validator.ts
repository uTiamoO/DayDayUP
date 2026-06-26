/**
 * 验证邮箱格式
 */
export function isEmail(email: string): boolean {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

/**
 * 验证手机号（中国大陆）
 */
export function isMobile(mobile: string): boolean {
  const mobileRegex = /^1[3-9]\d{9}$/;
  return mobileRegex.test(mobile);
}

/**
 * 验证 URL 格式
 */
export function isUrl(url: string): boolean {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
}

/**
 * 验证身份证号（中国大陆）
 */
export function isIdCard(idCard: string): boolean {
  const idCardRegex = /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/;
  return idCardRegex.test(idCard);
}

/**
 * 验证密码强度（至少包含字母和数字，长度8-20位）
 */
export function isStrongPassword(password: string): boolean {
  const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*?&]{8,20}$/;
  return passwordRegex.test(password);
}

/**
 * 验证用户名（字母、数字、下划线，4-16位）
 */
export function isUsername(username: string): boolean {
  const usernameRegex = /^[a-zA-Z0-9_]{4,16}$/;
  return usernameRegex.test(username);
}
