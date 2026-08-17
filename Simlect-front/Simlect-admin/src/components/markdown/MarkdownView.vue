<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="md-preview-panel" v-if="modelValue">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="md-preview">
      <!-- [zh] 开始标签 `<MdPreview>` -->
      <MdPreview :noEcharts="true" previewTheme="vuepress" :codeFoldable="false" id="preview" :modelValue="modelValue"
        noMermaid />
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>
<script setup>


import { MdPreview, MdCatalog, config } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'

import screenfull from 'screenfull'

import highlight from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'

import * as prettier from 'prettier'
import parserMarkdown from 'prettier/plugins/markdown'

import katex from 'katex'

import LinkAttr from 'markdown-it-link-attributes'
config({
  editorExtensions: {
    prettier: {
      prettierInstance: prettier,
      parserMarkdownInstance: parserMarkdown,
    },
    highlight: {
      instance: highlight,
    },
    screenfull: {
      instance: screenfull,
    },
    katex: {
      instance: katex,
    },
  },
  markdownItPlugins(plugins) {
    return [
      ...plugins,
      {
        type: 'linkAttr',
        plugin: LinkAttr,
        options: {
          matcher(href) {
            
            
            return !href.startsWith('#')
          },
          attrs: {
            target: '_blank',
            // 防 tabnabbing：新窗口必须带 rel=noopener noreferrer
            rel: 'noopener noreferrer',
          },
        },
      },
    ]
  },
})
import {
  createApp,
  ref,
  reactive,
  getCurrentInstance,
  nextTick,
  onMounted,
  computed,
} from 'vue'
const { proxy } = getCurrentInstance()
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()
const props = defineProps({
  modelValue: {
    type: String,
  },
})
const scrollElement = document.documentElement
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.md-preview-panel {` */
.md-preview-panel {
  /* [zh] 样式规则 `background: #f0f2f5;` */
  background: #f0f2f5;
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `.md-preview {` */
  .md-preview {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `:deep(.md-editor-preview-wrapper) {` */
    :deep(.md-editor-preview-wrapper) {
      /* [zh] 样式规则 `padding: 10px;` */
      padding: 10px;
    /* [zh] 样式规则 `}` */
    }
    /* [zh] 样式规则 `:deep(.md-editor-preview) {` */
    :deep(.md-editor-preview) {
      /* [zh] 样式规则 `background: #f0f2f5;` */
      background: #f0f2f5;
      /* [zh] 样式规则 `.md-editor-code {` */
      .md-editor-code {
        /* [zh] 样式规则 `.md-editor-code-head {` */
        .md-editor-code-head {
          /* [zh] 样式规则 `z-index: 1;` */
          z-index: 1;
        /* [zh] 样式规则 `}` */
        }
        /* [zh] 样式规则 `pre {` */
        pre {
          /* [zh] 样式规则 `code {` */
          code {
            /* [zh] 样式规则 `.md-editor-code-block {` */
            .md-editor-code-block {
              /* [zh] 样式规则 `word-wrap: break-word;` */
              word-wrap: break-word;
              /* [zh] 样式规则 `white-space: break-spaces;` */
              white-space: break-spaces;
            /* [zh] 样式规则 `}` */
            }
          /* [zh] 样式规则 `}` */
          }
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }
      /* [zh] 样式规则 `img {` */
      img {
        /* [zh] 样式规则 `max-width: 100%;` */
        max-width: 100%;
      /* [zh] 样式规则 `}` */
      }
      /* [zh] 样式规则 `blockquote {` */
      blockquote {
        /* [zh] 样式规则 `border-left: 3px solid var(--text3);` */
        border-left: 3px solid var(--text3);
        /* [zh] 样式规则 `padding-left: 10px;` */
        padding-left: 10px;
        /* [zh] 样式规则 `p {` */
        p {
          /* [zh] 样式规则 `color: var(--text3);` */
          color: var(--text3);
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }
      /* [zh] 样式规则 `a {` */
      a {
        /* [zh] 样式规则 `color: var(--link2);` */
        color: var(--link2);
      /* [zh] 样式规则 `}` */
      }
      /* [zh] 样式声明 */
      h1,
      /* [zh] 样式声明 */
      h2,
      /* [zh] 样式声明 */
      h3,
      /* [zh] 样式声明 */
      h4,
      /* [zh] 样式声明 */
      h5,
      /* [zh] 样式规则 `h6 {` */
      h6 {
        /* [zh] 样式规则 `margin: 10px 0px 5px 0px;` */
        margin: 10px 0px 5px 0px;
      /* [zh] 样式规则 `}` */
      }
      /* [zh] 样式规则 `h1 {` */
      h1 {
        /* [zh] 样式规则 `font-size: 26px;` */
        font-size: 26px;
      /* [zh] 样式规则 `}` */
      }
      /* [zh] 样式规则 `h2 {` */
      h2 {
        /* [zh] 样式规则 `font-size: 22px;` */
        font-size: 22px;
      /* [zh] 样式规则 `}` */
      }
      /* [zh] 样式规则 `h3 {` */
      h3 {
        /* [zh] 样式规则 `font-size: 20px;` */
        font-size: 20px;
      /* [zh] 样式规则 `}` */
      }
      /* [zh] 样式规则 `h4 {` */
      h4 {
        /* [zh] 样式规则 `font-size: 18px;` */
        font-size: 18px;
      /* [zh] 样式规则 `}` */
      }
      /* [zh] 样式规则 `h5 {` */
      h5 {
        /* [zh] 样式规则 `font-size: 16px;` */
        font-size: 16px;
      /* [zh] 样式规则 `}` */
      }
      /* [zh] 样式规则 `h6 {` */
      h6 {
        /* [zh] 样式规则 `font-size: 14px;` */
        font-size: 14px;
      /* [zh] 样式规则 `}` */
      }
      /* [zh] 样式规则 `a {` */
      a {
        /* [zh] 样式规则 `color: var(--link2);` */
        color: var(--link2);
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
