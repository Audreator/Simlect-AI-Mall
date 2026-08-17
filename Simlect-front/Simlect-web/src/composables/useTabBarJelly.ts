import { ref } from 'vue';

const JELLY_HOLD_MS = 260;

export function useTabBarJelly() {
  const barJelly = ref(false);
  const glassJelly = ref(false);
  let hideTimer: ReturnType<typeof setTimeout> | null = null;

  const onTabPress = () => {
    barJelly.value = false;
    glassJelly.value = false;

    requestAnimationFrame(() => {
      barJelly.value = true;
      glassJelly.value = true;
    });

    if (hideTimer) clearTimeout(hideTimer);
    hideTimer = setTimeout(() => {
      barJelly.value = false;
      glassJelly.value = false;
    }, JELLY_HOLD_MS);
  };

  return { barJelly, glassJelly, onTabPress };
}
