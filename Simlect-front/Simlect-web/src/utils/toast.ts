// [zh] 导入 `element-plus`
import { ElMessage } from 'element-plus';

// [zh] TypeScript 类型 `ToastType`（类比 Java interface/DTO）
type ToastType = 'success' | 'warning' | 'error' | 'info';


// [zh] 声明变量 `show`
const show = (message: string, type: ToastType = 'success') => {
  // [zh] 代码块开始
  ElMessage({
    // [zh] TypeScript/JS 语句：`message,`
    message,
    // [zh] TypeScript/JS 语句：`type,`
    type,
    // [zh] TypeScript/JS 语句：`duration: 2400,`
    duration: 2400,
    // [zh] TypeScript/JS 语句：`showClose: false,`
    showClose: false,
    // [zh] TypeScript/JS 语句：`customClass: `eshop-toast eshop-toast--${type}`,`
    customClass: `eshop-toast eshop-toast--${type}`,
    // [zh] TypeScript/JS 语句：`offset: 72,`
    offset: 72,
    // [zh] TypeScript/JS 语句：`grouping: true`
    grouping: true
  // [zh] 语句
  });
// [zh] 语句
};

// [zh] 导出模块成员
export const toast = {
  // [zh] TypeScript/JS 语句：`success: (msg: string) => show(msg, 'success'),`
  success: (msg: string) => show(msg, 'success'),
  // [zh] TypeScript/JS 语句：`warning: (msg: string) => show(msg, 'warning'),`
  warning: (msg: string) => show(msg, 'warning'),
  // [zh] TypeScript/JS 语句：`error: (msg: string) => show(msg, 'error'),`
  error: (msg: string) => show(msg, 'error'),
  // [zh] TypeScript/JS 语句：`info: (msg: string) => show(msg, 'info')`
  info: (msg: string) => show(msg, 'info')
// [zh] 语句
};
