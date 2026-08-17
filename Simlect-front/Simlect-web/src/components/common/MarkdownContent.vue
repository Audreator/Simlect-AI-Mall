<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div
    v-if="html"
    ref="rootRef"
    class="markdown-content"
    :class="{ 'is-image-center': centerImages }"
    v-html="html"
    @click="onContentClick"
  />
  <!-- [zh] 开始标签 `<p>` -->
  <p v-else class="markdown-empty">{{ emptyText }}</p>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import MarkdownIt from 'markdown-it';
import { openImagePreview } from '@/composables/imagePreview';
import { renderAgentMessageHtml } from '@/utils/agentMessageRender';
import { normalizeProductDesc } from '@/utils/productDesc';

const props = withDefaults(
  defineProps<{
    content?: string | null;
    emptyText?: string;

    agentRich?: boolean;

    centerImages?: boolean;
  }>(),
  { emptyText: '暂无详情', agentRich: false, centerImages: false }
);

const md = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true
});

const html = computed(() => {
  const normalized = normalizeProductDesc(props.content);
  if (!normalized) return '';
  try {
    if (props.agentRich) return renderAgentMessageHtml(normalized);
    return md.render(normalized);
  } catch (err) {
    console.warn('Markdown 渲染失败，已降级为纯文本', err);
    return normalized
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\n/g, '<br>');
  }
});

const rootRef = ref<HTMLElement>();

const onContentClick = (event: MouseEvent) => {
  const target = event.target;
  if (!(target instanceof Element)) return;
  const img = target.closest('img');
  if (!(img instanceof HTMLImageElement) || !img.src) return;
  event.preventDefault();
  const images = rootRef.value?.querySelectorAll('img');
  const urls = images ? Array.from(images).map((node) => node.src).filter(Boolean) : [img.src];
  const index = urls.indexOf(img.src);
  openImagePreview(urls, index >= 0 ? index : 0);
};
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.markdown-content {` */
.markdown-content {
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `line-height: 1.6;` */
  line-height: 1.6;
  /* [zh] 样式规则 `color: $color-text-body;` */
  color: $color-text-body;
  /* [zh] 样式规则 `word-break: break-word;` */
  word-break: break-word;

  /* [zh] 样式规则 `:deep(img) {` */
  :deep(img) {
    /* [zh] 样式规则 `display: block;` */
    display: block;
    /* [zh] 样式规则 `max-width: 100%;` */
    max-width: 100%;
    /* [zh] 样式规则 `height: auto;` */
    height: auto;
    /* [zh] 样式规则 `margin: 8px 0;` */
    margin: 8px 0;
    /* [zh] 样式规则 `border-radius: $radius-xs;` */
    border-radius: $radius-xs;
    /* [zh] 样式规则 `cursor: zoom-in;` */
    cursor: zoom-in;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(p) {` */
  :deep(p) {
    /* [zh] 样式规则 `margin: 8px 0;` */
    margin: 8px 0;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(a) {` */
  :deep(a) {
    /* [zh] 样式规则 `color: $color-primary;` */
    color: $color-primary;
    /* [zh] 样式规则 `text-decoration: none;` */
    text-decoration: none;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(table) {` */
  :deep(table) {
    /* [zh] 样式规则 `width: 100%;` */
    width: 100%;
    /* [zh] 样式规则 `margin: 10px 0;` */
    margin: 10px 0;
    /* [zh] 样式规则 `border-collapse: collapse;` */
    border-collapse: collapse;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `line-height: 1.45;` */
    line-height: 1.45;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  :deep(th),
  /* [zh] 样式规则 `:deep(td) {` */
  :deep(td) {
    /* [zh] 样式规则 `border: 1px solid $color-border;` */
    border: 1px solid $color-border;
    /* [zh] 样式规则 `padding: 8px 10px;` */
    padding: 8px 10px;
    /* [zh] 样式规则 `text-align: left;` */
    text-align: left;
    /* [zh] 样式规则 `vertical-align: top;` */
    vertical-align: top;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(th) {` */
  :deep(th) {
    /* [zh] 样式规则 `background: $color-bg-subtle;` */
    background: $color-bg-subtle;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;
    /* [zh] 样式规则 `font-weight: 600;` */
    font-weight: 600;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(td) {` */
  :deep(td) {
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `&.is-image-center {` */
  &.is-image-center {
    /* [zh] 样式规则 `:deep(img) {` */
    :deep(img) {
      /* [zh] 样式规则 `margin-left: auto;` */
      margin-left: auto;
      /* [zh] 样式规则 `margin-right: auto;` */
      margin-right: auto;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式声明 */
    :deep(p:has(> img:only-child)),
    /* [zh] 样式规则 `:deep(figure) {` */
    :deep(figure) {
      /* [zh] 样式规则 `text-align: center;` */
      text-align: center;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.markdown-empty {` */
.markdown-empty {
  /* [zh] 样式规则 `margin: 0;` */
  margin: 0;
  /* [zh] 样式规则 `font-size: 13px;` */
  font-size: 13px;
  /* [zh] 样式规则 `color: $color-text-muted;` */
  color: $color-text-muted;
/* [zh] 样式规则 `}` */
}
</style>
