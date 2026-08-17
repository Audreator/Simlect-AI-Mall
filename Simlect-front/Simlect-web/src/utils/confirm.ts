import { ElMessageBox } from 'element-plus';

export type ConfirmOptions = {
  title?: string;
  confirmButtonText?: string;
  cancelButtonText?: string;
  type?: 'warning' | 'info' | 'success' | 'error';
};


export async function confirmAction(message: string, options: ConfirmOptions = {}): Promise<boolean> {
  const {
    title = '请确认',
    confirmButtonText = '确定',
    cancelButtonText = '取消',
    type = 'warning'
  } = options;

  try {
    await ElMessageBox.confirm(message, title, {
      confirmButtonText,
      cancelButtonText,
      type,
      distinguishCancelAndClose: true,
      closeOnClickModal: false
    });
    return true;
  } catch {
    return false;
  }
}
