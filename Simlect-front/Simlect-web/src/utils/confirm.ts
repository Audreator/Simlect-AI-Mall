// [zh] 导入 `element-plus`
import { ElMessageBox } from 'element-plus';

// [zh] 导出模块成员
export type ConfirmOptions = {
  // [zh] 语句
  title?: string;
  // [zh] 语句
  confirmButtonText?: string;
  // [zh] 语句
  cancelButtonText?: string;
  // [zh] 语句
  type?: 'warning' | 'info' | 'success' | 'error';
// [zh] 语句
};


// [zh] 导出模块成员
export async function confirmAction(message: string, options: ConfirmOptions = {}): Promise<boolean> {
  // [zh] 代码块开始
  const {
    // [zh] TypeScript/JS 语句：`title = '请确认',`
    title = '请确认',
    // [zh] TypeScript/JS 语句：`confirmButtonText = '确定',`
    confirmButtonText = '确定',
    // [zh] TypeScript/JS 语句：`cancelButtonText = '取消',`
    cancelButtonText = '取消',
    // [zh] TypeScript 类型 ``（类比 Java interface/DTO）
    type = 'warning'
  // [zh] 语句
  } = options;

  // [zh] 代码块开始
  try {
    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await ElMessageBox.confirm(message, title, {
      // [zh] TypeScript/JS 语句：`confirmButtonText,`
      confirmButtonText,
      // [zh] TypeScript/JS 语句：`cancelButtonText,`
      cancelButtonText,
      // [zh] TypeScript/JS 语句：`type,`
      type,
      // [zh] TypeScript/JS 语句：`distinguishCancelAndClose: true,`
      distinguishCancelAndClose: true,
      // [zh] TypeScript/JS 语句：`closeOnClickModal: false`
      closeOnClickModal: false
    // [zh] 语句
    });
    // [zh] return 返回
    return true;
  // [zh] 代码块开始
  } catch {
    // [zh] return 返回
    return false;
  // [zh] 语法符号
  }
// [zh] 语法符号
}
