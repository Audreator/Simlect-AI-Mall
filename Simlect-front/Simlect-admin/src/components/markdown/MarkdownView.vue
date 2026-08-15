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
// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>


// [zh] 导入 `md-editor-v3`
import { MdPreview, MdCatalog, config } from 'md-editor-v3'
// [zh] 导入 `模块`
import 'md-editor-v3/lib/style.css'

// [zh] 导入 `screenfull`
import screenfull from 'screenfull'

// [zh] 导入 `highlight.js`
import highlight from 'highlight.js'
// [zh] 导入 `模块`
import 'highlight.js/styles/atom-one-dark.css'

// [zh] 导入 `prettier`
import * as prettier from 'prettier'
// [zh] 导入 `prettier/plugins/markdown`
import parserMarkdown from 'prettier/plugins/markdown'

// [zh] 导入 `katex`
import katex from 'katex'

// [zh] 导入 `markdown-it-link-attributes`
import LinkAttr from 'markdown-it-link-attributes'
// [zh] 代码块开始
config({
  // [zh] 代码块开始
  editorExtensions: {
    // [zh] 代码块开始
    prettier: {
      // [zh] TypeScript/JS 语句：`prettierInstance: prettier,`
      prettierInstance: prettier,
      // [zh] TypeScript/JS 语句：`parserMarkdownInstance: parserMarkdown,`
      parserMarkdownInstance: parserMarkdown,
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] 代码块开始
    highlight: {
      // [zh] TypeScript/JS 语句：`instance: highlight,`
      instance: highlight,
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] 代码块开始
    screenfull: {
      // [zh] TypeScript/JS 语句：`instance: screenfull,`
      instance: screenfull,
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] 代码块开始
    katex: {
      // [zh] TypeScript/JS 语句：`instance: katex,`
      instance: katex,
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  markdownItPlugins(plugins) {
    // [zh] return 返回
    return [
      // [zh] TypeScript/JS 语句：`...plugins,`
      ...plugins,
      // [zh] 代码块开始
      {
        // [zh] TypeScript/JS 语句：`type: 'linkAttr',`
        type: 'linkAttr',
        // [zh] TypeScript/JS 语句：`plugin: LinkAttr,`
        plugin: LinkAttr,
        // [zh] 代码块开始
        options: {
          // [zh] 代码块开始
          matcher(href) {
            
            
            // [zh] return 返回
            return !href.startsWith('#')
          // [zh] TypeScript/JS 语句：`},`
          },
          // [zh] 代码块开始
          attrs: {
            // [zh] TypeScript/JS 语句：`target: '_blank',`
            target: '_blank',
            // 防 tabnabbing：新窗口必须带 rel=noopener noreferrer
            rel: 'noopener noreferrer',
          // [zh] TypeScript/JS 语句：`},`
          },
        // [zh] TypeScript/JS 语句：`},`
        },
      // [zh] TypeScript/JS 语句：`},`
      },
    // [zh] TypeScript/JS 语句：`]`
    ]
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`})`
})
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`createApp,`
  createApp,
  // [zh] TypeScript/JS 语句：`ref,`
  ref,
  // [zh] TypeScript/JS 语句：`reactive,`
  reactive,
  // [zh] TypeScript/JS 语句：`getCurrentInstance,`
  getCurrentInstance,
  // [zh] TypeScript/JS 语句：`nextTick,`
  nextTick,
  // [zh] TypeScript/JS 语句：`onMounted,`
  onMounted,
  // [zh] TypeScript/JS 语句：`computed,`
  computed,
// [zh] TypeScript/JS 语句：`} from 'vue'`
} from 'vue'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 导入 `vue-router`
import { useRoute, useRouter } from 'vue-router'
// [zh] 声明变量 `route`
const route = useRoute()
// [zh] 声明变量 `router`
const router = useRouter()
// [zh] 声明变量 `props`
const props = defineProps({
  // [zh] 代码块开始
  modelValue: {
    // [zh] TypeScript/JS 语句：`type: String,`
    type: String,
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`})`
})
// [zh] 声明变量 `scrollElement`
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
