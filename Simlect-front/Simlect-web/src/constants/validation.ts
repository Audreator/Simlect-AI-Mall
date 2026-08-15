
// [zh] 导出模块成员
export const REGEX_PASSWORD = /^(?=.*\d)(?=.*[a-zA-Z])[\da-zA-Z~!@#$%^&*_]{8,18}$/;

// [zh] 导出模块成员
export const PASSWORD_FORMAT_HINT =
  // [zh] 语句
  '密码需 8-18 位，且同时包含字母和数字，可含特殊字符 ~!@#$%^&*_';

// [zh] 导出模块成员
export const isValidPassword = (password: string) => REGEX_PASSWORD.test(password);

// [zh] 导出模块成员
export const REGEX_EMAIL = /^[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}$/;

// [zh] 导出模块成员
export const isValidEmail = (email: string) => REGEX_EMAIL.test(email.trim());

// [zh] 导出模块成员
export const MAX_CART_QTY = 99;

