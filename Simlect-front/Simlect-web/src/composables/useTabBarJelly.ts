// [zh] 导入 `vue`
import { ref } from 'vue';

// [zh] 声明变量 `JELLY_HOLD_MS`
const JELLY_HOLD_MS = 260;

// [zh] 导出模块成员
export function useTabBarJelly() {
  // [zh] 声明变量 `barJelly`
  const barJelly = ref(false);
  // [zh] 声明变量 `glassJelly`
  const glassJelly = ref(false);
  // [zh] 声明变量 `hideTimer`
  let hideTimer: ReturnType<typeof setTimeout> | null = null;

  // [zh] 声明变量 `onTabPress`
  const onTabPress = () => {
    // [zh] 语句
    barJelly.value = false;
    // [zh] 语句
    glassJelly.value = false;

    // [zh] 代码块开始
    requestAnimationFrame(() => {
      // [zh] 语句
      barJelly.value = true;
      // [zh] 语句
      glassJelly.value = true;
    // [zh] 语句
    });

    // [zh] 条件判断 if
    if (hideTimer) clearTimeout(hideTimer);
    // [zh] 代码块开始
    hideTimer = setTimeout(() => {
      // [zh] 语句
      barJelly.value = false;
      // [zh] 语句
      glassJelly.value = false;
    // [zh] 语句
    }, JELLY_HOLD_MS);
  // [zh] 语句
  };

  // [zh] return 返回
  return { barJelly, glassJelly, onTabPress };
// [zh] 语法符号
}
