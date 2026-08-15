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

// [zh] TypeScript/JS 语句：`<script setup lang="ts">`
<script setup lang="ts">
// [zh] 导入 `vue`
import { computed, ref } from 'vue';
// [zh] 导入 `markdown-it`
import MarkdownIt from 'markdown-it';
// [zh] 导入 `@/composables/imagePreview`
import { openImagePreview } from '@/composables/imagePreview';
// [zh] 导入 `@/utils/agentMessageRender`
import { renderAgentMessageHtml } from '@/utils/agentMessageRender';
// [zh] 导入 `@/utils/productDesc`
import { normalizeProductDesc } from '@/utils/productDesc';

// [zh] 声明变量 `props`
const props = withDefaults(
  // [zh] 代码块开始
  defineProps<{
    // [zh] 语句
    content?: string | null;
    // [zh] 语句
    emptyText?: string;

    // [zh] 语句
    agentRich?: boolean;

    // [zh] 语句
    centerImages?: boolean;
  // [zh] TypeScript/JS 语句：`}>(),`
  }>(),
  // [zh] TypeScript/JS 语句：`{ emptyText: '暂无详情', agentRich: false, centerImages: fa`
  { emptyText: '暂无详情', agentRich: false, centerImages: false }
// [zh] 语句
);

// [zh] 声明变量 `md`
const md = new MarkdownIt({
  // [zh] TypeScript/JS 语句：`html: false,`
  html: false,
  // [zh] TypeScript/JS 语句：`breaks: true,`
  breaks: true,
  // [zh] TypeScript/JS 语句：`linkify: true`
  linkify: true
// [zh] 语句
});

// [zh] 声明变量 `html`
const html = computed(() => {
  // [zh] 声明变量 `normalized`
  const normalized = normalizeProductDesc(props.content);
  // [zh] 条件判断 if
  if (!normalized) return '';
  // [zh] 代码块开始
  try {
    // [zh] 条件判断 if
    if (props.agentRich) return renderAgentMessageHtml(normalized);
    // [zh] return 返回
    return md.render(normalized);
  // [zh] 代码块开始
  } catch (err) {
    // [zh] 语句
    console.warn('Markdown 渲染失败，已降级为纯文本', err);
    // [zh] return 返回
    return normalized
      // [zh] TypeScript/JS 语句：`.replace(/&/g, '&amp;')`
      .replace(/&/g, '&amp;')
      // [zh] TypeScript/JS 语句：`.replace(/</g, '&lt;')`
      .replace(/</g, '&lt;')
      // [zh] TypeScript/JS 语句：`.replace(/>/g, '&gt;')`
      .replace(/>/g, '&gt;')
      // [zh] 语句
      .replace(/\n/g, '<br>');
  // [zh] 语法符号
  }
// [zh] 语句
});

// [zh] 声明变量 `rootRef`
const rootRef = ref<HTMLElement>();

// [zh] 声明变量 `onContentClick`
const onContentClick = (event: MouseEvent) => {
  // [zh] 声明变量 `target`
  const target = event.target;
  // [zh] 条件判断 if
  if (!(target instanceof Element)) return;
  // [zh] 声明变量 `img`
  const img = target.closest('img');
  // [zh] 条件判断 if
  if (!(img instanceof HTMLImageElement) || !img.src) return;
  // [zh] 语句
  event.preventDefault();
  // [zh] 声明变量 `images`
  const images = rootRef.value?.querySelectorAll('img');
  // [zh] 声明变量 `urls`
  const urls = images ? Array.from(images).map((node) => node.src).filter(Boolean) : [img.src];
  // [zh] 声明变量 `index`
  const index = urls.indexOf(img.src);
  // [zh] 语句
  openImagePreview(urls, index >= 0 ? index : 0);
// [zh] 语句
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
