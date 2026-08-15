// [zh] 导入 `vue`
import { reactive } from 'vue';

// [zh] 导出模块成员
export const imagePreviewState = reactive({
  // [zh] TypeScript/JS 语句：`visible: false,`
  visible: false,
  // [zh] TypeScript/JS 语句：`urls: [] as string[],`
  urls: [] as string[],
  // [zh] TypeScript/JS 语句：`index: 0`
  index: 0
// [zh] 语句
});

// [zh] 导出模块成员
export const openImagePreview = (urls: string | string[], index = 0) => {
  // [zh] 声明变量 `list`
  const list = (Array.isArray(urls) ? urls : [urls]).map((u) => u.trim()).filter(Boolean);
  // [zh] 条件判断 if
  if (!list.length) return;
  // [zh] 语句
  imagePreviewState.urls = list;
  // [zh] 语句
  imagePreviewState.index = Math.min(Math.max(index, 0), list.length - 1);
  // [zh] 语句
  imagePreviewState.visible = true;
// [zh] 语句
};

// [zh] 导出模块成员
export const closeImagePreview = () => {
  // [zh] 语句
  imagePreviewState.visible = false;
// [zh] 语句
};
