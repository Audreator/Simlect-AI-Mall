import { ElMessage } from 'element-plus';

type ToastType = 'success' | 'warning' | 'error' | 'info';


const show = (message: string, type: ToastType = 'success') => {
  ElMessage({
    message,
    type,
    duration: 2400,
    showClose: false,
    customClass: `eshop-toast eshop-toast--${type}`,
    offset: 72,
    grouping: true
  });
};

export const toast = {
  success: (msg: string) => show(msg, 'success'),
  warning: (msg: string) => show(msg, 'warning'),
  error: (msg: string) => show(msg, 'error'),
  info: (msg: string) => show(msg, 'info')
};
