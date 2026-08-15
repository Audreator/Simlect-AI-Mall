<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<Drawer>` -->
  <Drawer :show="dialogConfig.show" :title="dialogConfig.title" :buttons="dialogConfig.buttons" width="80%"
    @close="dialogConfig.show = false">
    <!-- [zh] 开始标签 `<el-form>` -->
    <el-form :model="formData" :rules="rules" ref="formDataRef" label-width="70px" @submit.prevent>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="问题" prop="question">
        <!-- [zh] 开始标签 `<el-input>` -->
        <el-input clearable placeholder="请输入问题" v-model.trim="formData.question"></el-input>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="相似问题" prop="similarQuestion">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="similar-questions-panel">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button @click="addSimilarQuestion" type="primary">增加相似问题</el-button>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="similar-questions-list">
            <!-- [zh] 开始标签 `<div>` -->
            <div class="similar-question-item" v-for="(question, index) in formData.similarQuestion" :key="index">
              <!-- [zh] 开始标签 `<el-input>` -->
              <el-input v-model="formData.similarQuestion[index]" placeholder="请输入相似问题" clearable>
              <!-- [zh] 闭合标签 `</el-input>` -->
              </el-input>
              <!-- [zh] 开始标签 `<div>` -->
              <div class="iconfont icon-delete" @click="delSimilarQuestion(index)"></div>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
      <!-- [zh] 开始标签 `<el-form-item>` -->
      <el-form-item label="答案" prop="answer">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="editor-panel">
          <!-- [zh] 开始标签 `<EditorMarkdown>` -->
          <EditorMarkdown v-model="formData.answer"></EditorMarkdown>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</el-form-item>` -->
      </el-form-item>
    <!-- [zh] 闭合标签 `</el-form>` -->
    </el-form>
  <!-- [zh] 闭合标签 `</Drawer>` -->
  </Drawer>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `@/components/markdown/EditorMarkdown.vue`
import EditorMarkdown from '@/components/markdown/EditorMarkdown.vue'
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
// [zh] 导入 `vue-router`
import { useRouter, useRoute } from 'vue-router'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `router`
const router = useRouter()
// [zh] 声明变量 `route`
const route = useRoute()

// [zh] 声明变量 `dialogConfig`
const dialogConfig = ref({
  // [zh] TypeScript/JS 语句：`show: false,`
  show: false,
  // [zh] TypeScript/JS 语句：`title: '编辑问题',`
  title: '编辑问题',
  // [zh] TypeScript/JS 语句：`buttons: [`
  buttons: [
    // [zh] 代码块开始
    {
      // [zh] TypeScript/JS 语句：`type: 'primary',`
      type: 'primary',
      // [zh] TypeScript/JS 语句：`text: '保存',`
      text: '保存',
      // [zh] 代码块开始
      click: (e) => {
        // [zh] TypeScript/JS 语句：`sumitForm()`
        sumitForm()
      // [zh] TypeScript/JS 语句：`},`
      },
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] 语法符号
  ],
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `show`
const show = async (data = {}) => {
  // [zh] TypeScript/JS 语句：`dialogConfig.value.show = true`
  dialogConfig.value.show = true
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await nextTick()
  // [zh] TypeScript/JS 语句：`formDataRef.value.resetFields()`
  formDataRef.value.resetFields()
  // [zh] TypeScript/JS 语句：`formData.value = { ...data }`
  formData.value = { ...data }
  // [zh] 条件判断 if
  if (!formData.value.similarQuestion) {
    // [zh] TypeScript/JS 语句：`formData.value.similarQuestion = []`
    formData.value.similarQuestion = []
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 代码块开始
defineExpose({
  // [zh] TypeScript/JS 语句：`show,`
  show,
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `formData`
const formData = ref({
  // [zh] TypeScript/JS 语句：`similarQuestion: [],`
  similarQuestion: [],
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 声明变量 `formDataRef`
const formDataRef = ref()
// [zh] 声明变量 `rules`
const rules = {
  // [zh] TypeScript/JS 语句：`question: [{ required: true, message: '请输入问题', trigger:`
  question: [{ required: true, message: '请输入问题', trigger: 'blur' }],
  // [zh] TypeScript/JS 语句：`answer: [{ required: true, message: '请输入答案', trigger: '`
  answer: [{ required: true, message: '请输入答案', trigger: 'blur' }],
  // [zh] TypeScript/JS 语句：`similarQuestion: [`
  similarQuestion: [
    // [zh] 代码块开始
    {
      // [zh] 代码块开始
      validator: (rule, value, callback) => {
        // [zh] 条件判断 if
        if (value.length == 0) {
          // [zh] TypeScript/JS 语句：`callback()`
          callback()
          // [zh] TypeScript/JS 语句：`return`
          return
        // [zh] 语法符号
        }
        // [zh] 声明变量 `empty`
        const empty = value.find((item) => {
          // [zh] return 返回
          return item.trim() === ''
        // [zh] TypeScript/JS 语句：`})`
        })
        // [zh] 条件判断 if
        if (empty != null) {
          // [zh] TypeScript/JS 语句：`callback(new Error('相似问题不能为空'))`
          callback(new Error('相似问题不能为空'))
          // [zh] TypeScript/JS 语句：`return`
          return
        // [zh] 语法符号
        }
        // [zh] 声明变量 `uniqueQuestions`
        const uniqueQuestions = [...new Set(value)]
        // [zh] 条件判断 if
        if (uniqueQuestions.length !== value.length) {
          // [zh] TypeScript/JS 语句：`callback(new Error('存在重复的相似问题'))`
          callback(new Error('存在重复的相似问题'))
        // [zh] 代码块开始
        } else {
          // [zh] TypeScript/JS 语句：`callback()`
          callback()
        // [zh] 语法符号
        }
      // [zh] TypeScript/JS 语句：`},`
      },
      // [zh] TypeScript/JS 语句：`trigger: 'blur',`
      trigger: 'blur',
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] 语法符号
  ],
// [zh] 语法符号
}


// [zh] 声明变量 `addSimilarQuestion`
const addSimilarQuestion = () => {
  // [zh] TypeScript/JS 语句：`formData.value.similarQuestion.push('')`
  formData.value.similarQuestion.push('')
// [zh] 语法符号
}


// [zh] 声明变量 `delSimilarQuestion`
const delSimilarQuestion = (index) => {
  // [zh] TypeScript/JS 语句：`formData.value.similarQuestion.splice(index, 1)`
  formData.value.similarQuestion.splice(index, 1)
// [zh] 语法符号
}

// [zh] 声明变量 `emit`
const emit = defineEmits(['reload'])
// [zh] 声明变量 `sumitForm`
const sumitForm = () => {
  // [zh] 代码块开始
  formDataRef.value.validate(async (valid) => {
    // [zh] 条件判断 if
    if (!valid) {
      // [zh] TypeScript/JS 语句：`return`
      return
    // [zh] 语法符号
    }
    // [zh] 声明变量 `params`
    let params = {}
    // [zh] TypeScript/JS 语句：`Object.assign(params, formData.value)`
    Object.assign(params, formData.value)
    // [zh] 条件判断 if
    if (params.similarQuestion.length > 0) {
      // [zh] TypeScript/JS 语句：`params.similarQuestion = JSON.stringify(params.similarQ`
      params.similarQuestion = JSON.stringify(params.similarQuestion)
    // [zh] 代码块开始
    } else {
      // [zh] TypeScript/JS 语句：`delete params.similarQuestion`
      delete params.similarQuestion
    // [zh] 语法符号
    }
    // [zh] 声明变量 `result`
    let result = await proxy.Request({
      // [zh] TypeScript/JS 语句：`url: proxy.Api.saveRagQuestion,`
      url: proxy.Api.saveRagQuestion,
      // [zh] TypeScript/JS 语句：`params,`
      params,
    // [zh] TypeScript/JS 语句：`})`
    })
    // [zh] 条件判断 if
    if (!result) {
      // [zh] TypeScript/JS 语句：`return`
      return
    // [zh] 语法符号
    }
    // [zh] TypeScript/JS 语句：`dialogConfig.value.show = false`
    dialogConfig.value.show = false
    // [zh] TypeScript/JS 语句：`proxy.Message.success('保存成功')`
    proxy.Message.success('保存成功')
    // [zh] TypeScript/JS 语句：`emit('reload')`
    emit('reload')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.similar-questions-panel {` */
.similar-questions-panel {
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;

  /* [zh] 样式规则 `.similar-questions-list {` */
  .similar-questions-list {
    /* [zh] 样式规则 `max-height: 158px;` */
    max-height: 158px;
    /* [zh] 样式规则 `overflow: auto;` */
    overflow: auto;
    /* [zh] 样式规则 `padding: 5px 10px 5px 0px;` */
    padding: 5px 10px 5px 0px;

    /* [zh] 样式规则 `&::-webkit-scrollbar {` */
    &::-webkit-scrollbar {
      /* [zh] 样式规则 `width: 4px;` */
      width: 4px;
      /* [zh] 样式规则 `height: 4px;` */
      height: 4px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.similar-question-item {` */
    .similar-question-item {
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `margin-bottom: 8px;` */
      margin-bottom: 8px;

      /* [zh] 样式规则 `.icon-delete {` */
      .icon-delete {
        /* [zh] 样式规则 `cursor: pointer;` */
        cursor: pointer;
        /* [zh] 样式规则 `margin-left: 10px;` */
        margin-left: 10px;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.editor-panel {` */
.editor-panel {
  /* [zh] 样式规则 `height: calc(100vh - 390px);` */
  height: calc(100vh - 390px);
  /* [zh] 样式规则 `width: 100%;` */
  width: 100%;
/* [zh] 样式规则 `}` */
}
</style>
