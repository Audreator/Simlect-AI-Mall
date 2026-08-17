<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<MdEditor>` -->
  <MdEditor :noEcharts="true" :toolbars="toolbars" :modelValue="modelValue" previewTheme="vuepress"
    :codeFoldable="false" :noPrettier="true" :showToolbarName="true" @onChange="change" @onUploadImg="onUploadImg"
    @onHtmlChanged="htmlChanged"></MdEditor>
</template>

<script setup>


import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
const { proxy } = getCurrentInstance()
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()

import Request from '@/utils/Request.js'
import { Api } from '@/utils/Api.js'
import { MdEditor, config } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'

import screenfull from 'screenfull'
import highlight from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'

import * as prettier from 'prettier'
import parserMarkdown from 'prettier/plugins/markdown'

import LinkAttr from 'markdown-it-link-attributes'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  height: {
    type: Number,
    default: 500,
  },
})
const toolbars = [
  'revoke',
  'next',
  '-',
  'bold',
  'underline',
  'italic',
  '-',
  'strikeThrough',
  'title',
  'quote',
  'unorderedList',
  'orderedList',
  'task', 
  '-',
  'image',
  'table',
  '=',
  'pageFullscreen',
  'preview',
  'previewOnly',
]

config({
  editorConfig: {
    renderDelay: 100,
  },
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
          },
        },
      },
    ]
  },
  codeMirrorExtensions(extensions) {
    return extensions.map((item) => {
      if (item.type === 'linkShortener') {
        return {
          ...item,
          options: {
            maxLength: 1000,
            shortenText: (url) => '...',
          },
        }
      }
      return item
    })
  },
})

const emit = defineEmits(['update:modelValue'])
const change = (e) => {
  emit('update:modelValue', e)
}

const textContent = ref()
const htmlChanged = (e) => {
  textContent.value = e
    .replace(/<[^>]*>/g, '') 
    .replace(/&nbsp;/gi, '')
    .replace(/\n/gi, '')
}

const getTextContent = () => {
  return textContent.value
}

defineExpose({
  getTextContent,
})

const onUploadImg = async (files, callback) => {
  const res = await Promise.all(
    files.map((file) => {
      return new Promise(async (rev, rej) => {
        const result = await Request({
          url: Api.uploadImage,
          params: {
            file: file,
          },
        })
        if (!result) {
          return
        }
        rev(Api.sourcePath + result.data)
      })
    })
  )
  callback(res)
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
