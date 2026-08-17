import { ref } from 'vue';
import type SlideCaptchaDialog from '@/components/business/SlideCaptchaDialog.vue';

export function useEmailCode() {
  const emailCodeCountdown = ref(0);
  let timer: ReturnType<typeof setInterval> | null = null;

  const emailCodeBtnText = () => {
    if (emailCodeCountdown.value <= 0) return '发送验证码';
    return `${emailCodeCountdown.value}s后重发`;
  };

  const startCountdown = () => {
    if (emailCodeCountdown.value > 0) return;
    emailCodeCountdown.value = 60;
    timer = setInterval(() => {
      emailCodeCountdown.value--;
      if (emailCodeCountdown.value <= 0) {
        clearTimer();
      }
    }, 1000);
  };

  const clearTimer = () => {
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
    emailCodeCountdown.value = 0;
  };

  const requestSlideVerification = async (
    slideRef: { open: () => Promise<string> } | null | undefined
  ) => {
    if (!slideRef?.open) {
      throw new Error('验证组件未就绪');
    }
    return slideRef.open();
  };

  return { emailCodeCountdown, emailCodeBtnText, startCountdown, clearTimer, requestSlideVerification };
}

export type SlideCaptchaDialogExpose = InstanceType<typeof SlideCaptchaDialog>;
