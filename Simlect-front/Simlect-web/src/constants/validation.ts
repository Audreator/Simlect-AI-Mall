
export const REGEX_PASSWORD = /^(?=.*\d)(?=.*[a-zA-Z])[\da-zA-Z~!@#$%^&*_]{8,18}$/;

export const PASSWORD_FORMAT_HINT =
  '密码需 8-18 位，且同时包含字母和数字，可含特殊字符 ~!@#$%^&*_';

export const isValidPassword = (password: string) => REGEX_PASSWORD.test(password);

export const REGEX_EMAIL = /^[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}$/;

export const isValidEmail = (email: string) => REGEX_EMAIL.test(email.trim());

export const MAX_CART_QTY = 99;

