<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<MdEditor>` -->
  <MdEditor :noEcharts="true" :toolbars="toolbars" :modelValue="modelValue" previewTheme="vuepress"
    :codeFoldable="false" :noPrettier="true" :showToolbarName="true" @onChange="change" @onUploadImg="onUploadImg"
    @onHtmlChanged="htmlChanged"></MdEditor>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>


// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 导入 `vue-router`
import { useRoute, useRouter } from 'vue-router'
// [zh] 声明变量 `route`
const route = useRoute()
// [zh] 声明变量 `router`
const router = useRouter()

// [zh] 导入 `@/utils/Request.js`
import Request from '@/utils/Request.js'
// [zh] 导入 `@/utils/Api.js`
import { Api } from '@/utils/Api.js'
// [zh] 导入 `md-editor-v3`
import { MdEditor, config } from 'md-editor-v3'
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

// [zh] 导入 `markdown-it-link-attributes`
import LinkAttr from 'markdown-it-link-attributes'

// [zh] 声明变量 `props`
const props = defineProps({
  // [zh] 代码块开始
  modelValue: {
    // [zh] TypeScript/JS 语句：`type: String,`
    type: String,
    // [zh] TypeScript/JS 语句：`default: '',`
    default: '',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  height: {
    // [zh] TypeScript/JS 语句：`type: Number,`
    type: Number,
    // [zh] TypeScript/JS 语句：`default: 500,`
    default: 500,
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`})`
})
// [zh] 声明变量 `toolbars`
const toolbars = [
  // [zh] TypeScript/JS 语句：`'revoke',`
  'revoke',
  // [zh] TypeScript/JS 语句：`'next',`
  'next',
  // [zh] TypeScript/JS 语句：`'-',`
  '-',
  // [zh] TypeScript/JS 语句：`'bold',`
  'bold',
  // [zh] TypeScript/JS 语句：`'underline',`
  'underline',
  // [zh] TypeScript/JS 语句：`'italic',`
  'italic',
  // [zh] TypeScript/JS 语句：`'-',`
  '-',
  // [zh] TypeScript/JS 语句：`'strikeThrough',`
  'strikeThrough',
  // [zh] TypeScript/JS 语句：`'title',`
  'title',
  // [zh] TypeScript/JS 语句：`'quote',`
  'quote',
  // [zh] TypeScript/JS 语句：`'unorderedList',`
  'unorderedList',
  // [zh] TypeScript/JS 语句：`'orderedList',`
  'orderedList',
  // [zh] TypeScript/JS 语句：`'task',`
  'task', 
  // [zh] TypeScript/JS 语句：`'-',`
  '-',
  // [zh] TypeScript/JS 语句：`'image',`
  'image',
  // [zh] TypeScript/JS 语句：`'table',`
  'table',
  // [zh] TypeScript/JS 语句：`'=',`
  '=',
  // [zh] TypeScript/JS 语句：`'pageFullscreen',`
  'pageFullscreen',
  // [zh] TypeScript/JS 语句：`'preview',`
  'preview',
  // [zh] TypeScript/JS 语句：`'previewOnly',`
  'previewOnly',
// [zh] TypeScript/JS 语句：`]`
]

// [zh] 代码块开始
config({
  // [zh] 代码块开始
  editorConfig: {
    // [zh] TypeScript/JS 语句：`renderDelay: 100,`
    renderDelay: 100,
  // [zh] TypeScript/JS 语句：`},`
  },
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
  // [zh] 代码块开始
  codeMirrorExtensions(extensions) {
    // [zh] return 返回
    return extensions.map((item) => {
      // [zh] 条件判断 if
      if (item.type === 'linkShortener') {
        // [zh] return 返回
        return {
          // [zh] TypeScript/JS 语句：`...item,`
          ...item,
          // [zh] 代码块开始
          options: {
            // [zh] TypeScript/JS 语句：`maxLength: 1000,`
            maxLength: 1000,
            // [zh] TypeScript/JS 语句：`shortenText: (url) => '...',`
            shortenText: (url) => '...',
          // [zh] TypeScript/JS 语句：`},`
          },
        // [zh] 语法符号
        }
      // [zh] 语法符号
      }
      // [zh] return 返回
      return item
    // [zh] TypeScript/JS 语句：`})`
    })
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `emit`
const emit = defineEmits(['update:modelValue'])
// [zh] 声明变量 `change`
const change = (e) => {
  // [zh] TypeScript/JS 语句：`emit('update:modelValue', e)`
  emit('update:modelValue', e)
// [zh] 语法符号
}

// [zh] 声明变量 `textContent`
const textContent = ref()
// [zh] 声明变量 `htmlChanged`
const htmlChanged = (e) => {
  // [zh] TypeScript/JS 语句：`textContent.value = e`
  textContent.value = e
    // [zh] TypeScript/JS 语句：`.replace(/<[^>]*>/g, '')`
    .replace(/<[^>]*>/g, '') 
    // [zh] TypeScript/JS 语句：`.replace(/&nbsp;/gi, '')`
    .replace(/&nbsp;/gi, '')
    // [zh] TypeScript/JS 语句：`.replace(/\n/gi, '')`
    .replace(/\n/gi, '')
// [zh] 语法符号
}

// [zh] 声明变量 `getTextContent`
const getTextContent = () => {
  // [zh] return 返回
  return textContent.value
// [zh] 语法符号
}

// [zh] 代码块开始
defineExpose({
  // [zh] TypeScript/JS 语句：`getTextContent,`
  getTextContent,
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `onUploadImg`
const onUploadImg = async (files, callback) => {
  // [zh] 声明变量 `res`
  const res = await Promise.all(
    // [zh] 代码块开始
    files.map((file) => {
      // [zh] return 返回
      return new Promise(async (rev, rej) => {
        // [zh] 声明变量 `result`
        const result = await Request({
          // [zh] TypeScript/JS 语句：`url: Api.uploadImage,`
          url: Api.uploadImage,
          // [zh] 代码块开始
          params: {
            // [zh] TypeScript/JS 语句：`file: file,`
            file: file,
          // [zh] TypeScript/JS 语句：`},`
          },
        // [zh] TypeScript/JS 语句：`})`
        })
        // [zh] 条件判断 if
        if (!result) {
          // [zh] TypeScript/JS 语句：`return`
          return
        // [zh] 语法符号
        }
        // [zh] TypeScript/JS 语句：`rev(Api.sourcePath + result.data)`
        rev(Api.sourcePath + result.data)
      // [zh] TypeScript/JS 语句：`})`
      })
    // [zh] TypeScript/JS 语句：`})`
    })
  // [zh] 语法符号
  )
  // [zh] TypeScript/JS 语句：`callback(res)`
  callback(res)
// [zh] 语法符号
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.md-editor {` */
.md-editor {
  /* [zh] 样式规则 `height: 100%;` */
  height: 100%;

  * {
    /* [zh] 样式规则 `box-sizing: content-box;` */
    box-sizing: content-box;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(p) {` */
  :deep(p) {
    /* [zh] 样式规则 `img {` */
    img {
      /* [zh] 样式规则 `vertical-align: bottom;` */
      vertical-align: bottom;
      
      /* [zh] 样式规则 `display: inline-block;` */
      display: inline-block;
      /* [zh] 样式规则 `margin: 0;` */
      margin: 0;
      /* [zh] 样式规则 `padding: 0;` */
      padding: 0;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(svg.md-editor-icon) {` */
  :deep(svg.md-editor-icon) {
    
    /* [zh] 样式规则 `margin: 0px auto;` */
    margin: 0px auto;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `:deep(.md-editor-toolbar-item-name) {` */
  :deep(.md-editor-toolbar-item-name) {
    /* [zh] 样式规则 `line-height: 20px;` */
    line-height: 20px;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式声明 */
div.vuepress-theme h1,
/* [zh] 样式声明 */
div.vuepress-theme h2,
/* [zh] 样式声明 */
div.vuepress-theme h3,
/* [zh] 样式声明 */
div.vuepress-theme h4,
/* [zh] 样式声明 */
div.vuepress-theme h5,
/* [zh] 样式规则 `div.vuepress-theme h6 {` */
div.vuepress-theme h6 {
  /* [zh] 样式规则 `margin: 0px !important;` */
  margin: 0px !important;
/* [zh] 样式规则 `}` */
}
</style>
