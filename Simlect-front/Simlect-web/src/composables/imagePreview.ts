import { reactive } from 'vue';

export const imagePreviewState = reactive({
  visible: false,
  urls: [] as string[],
  index: 0
});

export const openImagePreview = (urls: string | string[], index = 0) => {
  const list = (Array.isArray(urls) ? urls : [urls]).map((u) => u.trim()).filter(Boolean);
  if (!list.length) return;
  imagePreviewState.urls = list;
  imagePreviewState.index = Math.min(Math.max(index, 0), list.length - 1);
  imagePreviewState.visible = true;
};

export const closeImagePreview = () => {
  imagePreviewState.visible = false;
};
