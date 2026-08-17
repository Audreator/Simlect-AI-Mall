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

<script setup>
import EditorMarkdown from '@/components/markdown/EditorMarkdown.vue'
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const dialogConfig = ref({
  show: false,
  title: '编辑问题',
  buttons: [
    {
      type: 'primary',
      text: '保存',
      click: (e) => {
        sumitForm()
      },
    },
  ],
})

const show = async (data = {}) => {
  dialogConfig.value.show = true
  await nextTick()
  formDataRef.value.resetFields()
  formData.value = { ...data }
  if (!formData.value.similarQuestion) {
    formData.value.similarQuestion = []
  }
}

defineExpose({
  show,
})

const formData = ref({
  similarQuestion: [],
})

const formDataRef = ref()
const rules = {
  question: [{ required: true, message: '请输入问题', trigger: 'blur' }],
  answer: [{ required: true, message: '请输入答案', trigger: 'blur' }],
  similarQuestion: [
    {
      validator: (rule, value, callback) => {
        if (value.length == 0) {
          callback()
          return
        }
        const empty = value.find((item) => {
          return item.trim() === ''
        })
        if (empty != null) {
          callback(new Error('相似问题不能为空'))
          return
        }
        const uniqueQuestions = [...new Set(value)]
        if (uniqueQuestions.length !== value.length) {
          callback(new Error('存在重复的相似问题'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}


const addSimilarQuestion = () => {
  formData.value.similarQuestion.push('')
}


const delSimilarQuestion = (index) => {
  formData.value.similarQuestion.splice(index, 1)
}

const emit = defineEmits(['reload'])
const sumitForm = () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    let params = {}
    Object.assign(params, formData.value)
    if (params.similarQuestion.length > 0) {
      params.similarQuestion = JSON.stringify(params.similarQuestion)
    } else {
      delete params.similarQuestion
    }
    let result = await proxy.Request({
      url: proxy.Api.saveRagQuestion,
      params,
    })
    if (!result) {
      return
    }
    dialogConfig.value.show = false
    proxy.Message.success('保存成功')
    emit('reload')
    return
  })
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
