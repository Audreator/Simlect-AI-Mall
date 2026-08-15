// [zh] 导入 `vue`
import { ref } from 'vue';
// [zh] 导入 `模块`
import type SlideCaptchaDialog from '@/components/business/SlideCaptchaDialog.vue';

// [zh] 导出模块成员
export function useEmailCode() {
  // [zh] 声明变量 `emailCodeCountdown`
  const emailCodeCountdown = ref(0);
  // [zh] 声明变量 `timer`
  let timer: ReturnType<typeof setInterval> | null = null;

  // [zh] 声明变量 `emailCodeBtnText`
  const emailCodeBtnText = () => {
    // [zh] 条件判断 if
    if (emailCodeCountdown.value <= 0) return '发送验证码';
    // [zh] return 返回
    return `${emailCodeCountdown.value}s后重发`;
  // [zh] 语句
  };

  // [zh] 声明变量 `startCountdown`
  const startCountdown = () => {
    // [zh] 条件判断 if
    if (emailCodeCountdown.value > 0) return;
    // [zh] 语句
    emailCodeCountdown.value = 60;
    // [zh] 代码块开始
    timer = setInterval(() => {
      // [zh] 语句
      emailCodeCountdown.value--;
      // [zh] 条件判断 if
      if (emailCodeCountdown.value <= 0) {
        // [zh] 语句
        clearTimer();
      // [zh] 语法符号
      }
    // [zh] 语句
    }, 1000);
  // [zh] 语句
  };

  // [zh] 声明变量 `clearTimer`
  const clearTimer = () => {
    // [zh] 条件判断 if
    if (timer) {
      // [zh] 语句
      clearInterval(timer);
      // [zh] 语句
      timer = null;
    // [zh] 语法符号
    }
    // [zh] 语句
    emailCodeCountdown.value = 0;
  // [zh] 语句
  };

  // [zh] 声明变量 `requestSlideVerification`
  const requestSlideVerification = async (
    // [zh] TypeScript/JS 语句：`slideRef: { open: () => Promise<string> } | null | unde`
    slideRef: { open: () => Promise<string> } | null | undefined
  // [zh] 代码块开始
  ) => {
    // [zh] 条件判断 if
    if (!slideRef?.open) {
      // [zh] 语句
      throw new Error('验证组件未就绪');
    // [zh] 语法符号
    }
    // [zh] return 返回
    return slideRef.open();
  // [zh] 语句
  };

  // [zh] return 返回
  return { emailCodeCountdown, emailCodeBtnText, startCountdown, clearTimer, requestSlideVerification };
// [zh] 语法符号
}

// [zh] 导出模块成员
export type SlideCaptchaDialogExpose = InstanceType<typeof SlideCaptchaDialog>;
