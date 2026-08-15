<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="top-panel">
    <!-- [zh] 开始标签 `<el-form>` -->
    <el-form :model="searchForm" @submit.prevent>
      <!-- [zh] 开始标签 `<el-row>` -->
      <el-row :gutter="10">
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="5">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="用户ID">
            <!-- [zh] 开始标签 `<el-input>` -->
            <el-input clearable placeholder="用户ID" v-model="searchForm.userIdFuzzy" />
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="4">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="场景">
            <!-- [zh] 开始标签 `<el-select>` -->
            <el-select clearable placeholder="全部" v-model="searchForm.scene">
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option label="头像" value="avatar" />
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option label="评论" value="comment" />
            <!-- [zh] 闭合标签 `</el-select>` -->
            </el-select>
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="4">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="状态">
            <!-- [zh] 开始标签 `<el-select>` -->
            <el-select clearable placeholder="全部" v-model="searchForm.status">
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option label="待复核" :value="0" />
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option label="已通过" :value="1" />
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option label="确认违规" :value="2" />
              <!-- [zh] 开始标签 `<el-option>` -->
              <el-option label="误报驳回" :value="3" />
            <!-- [zh] 闭合标签 `</el-select>` -->
            </el-select>
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="5">
          <!-- [zh] 开始标签 `<el-button>` -->
          <el-button type="primary" @click="loadDataList">搜索</el-button>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
      <!-- [zh] 闭合标签 `</el-row>` -->
      </el-row>
    <!-- [zh] 闭合标签 `</el-form>` -->
    </el-form>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
  <!-- [zh] 开始标签 `<el-card>` -->
  <el-card class="table-data-card">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="table-panel">
      <!-- [zh] 开始标签 `<Table>` -->
      <Table ref="tableInfoRef" :columns="columns" :fetch="loadDataList" :dataSource="tableData">
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotImage="{ row }">
          <!-- [zh] 开始标签 `<img>` -->
          <img v-if="row.imagePath" class="thumb" :src="imageUrl(row.imagePath)" alt="" />
          <!-- [zh] 开始标签 `<span>` -->
          <span v-else>—</span>
        </template>
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotScene="{ row }">
          <!-- [zh] Mustache 插值表达式 -->
          {{ row.scene === 'avatar' ? '头像' : row.scene === 'comment' ? '评论' : row.scene || '—' }}
        </template>
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotStatus="{ row }">
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-if="row.status === 0" type="warning" size="small">待复核</el-tag>
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-else-if="row.status === 1" type="success" size="small">已通过</el-tag>
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-else-if="row.status === 2" type="danger" size="small">确认违规</el-tag>
          <!-- [zh] 开始标签 `<el-tag>` -->
          <el-tag v-else-if="row.status === 3" type="info" size="small">误报驳回</el-tag>
        </template>
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotOperation="{ row }">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="list-op-panel">
            <!-- [zh] 开始标签 `<OpBtn>` -->
            <OpBtn
              v-if="row.status === 0"
              icon="icon-edit"
              tips="复核"
              @click="openHandle(row)"
            />
            <!-- [zh] 开始标签 `<OpBtn>` -->
            <OpBtn
              v-else
              icon="icon-user"
              tips="用户解封"
              @click="openHandle(row)"
            />
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        </template>
      </Table>
    </div>
  </el-card>
  <HandleImageModeration ref="handleRef" @reload="loadDataList" />
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `./HandleImageModeration.vue`
import HandleImageModeration from './HandleImageModeration.vue'
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance } from 'vue'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()

// [zh] 声明变量 `columns`
const columns = [
  // [zh] TypeScript/JS 语句：`{ label: 'ID', prop: 'recordId', width: 70 },`
  { label: 'ID', prop: 'recordId', width: 70 },
  // [zh] TypeScript/JS 语句：`{ label: '用户ID', prop: 'userId', width: 120 },`
  { label: '用户ID', prop: 'userId', width: 120 },
  // [zh] TypeScript/JS 语句：`{ label: 'IP', prop: 'userIp', width: 120 },`
  { label: 'IP', prop: 'userIp', width: 120 },
  // [zh] TypeScript/JS 语句：`{ label: '场景', scopedSlots: 'slotScene', width: 80 },`
  { label: '场景', scopedSlots: 'slotScene', width: 80 },
  // [zh] TypeScript/JS 语句：`{ label: '图片', scopedSlots: 'slotImage', width: 90 },`
  { label: '图片', scopedSlots: 'slotImage', width: 90 },
  // [zh] TypeScript/JS 语句：`{ label: '百度结论', prop: 'conclusion', width: 160 },`
  { label: '百度结论', prop: 'conclusion', width: 160 },
  // [zh] TypeScript/JS 语句：`{ label: '状态', scopedSlots: 'slotStatus', width: 90 },`
  { label: '状态', scopedSlots: 'slotStatus', width: 90 },
  // [zh] TypeScript/JS 语句：`{ label: '上传时间', prop: 'createTime', width: 160 },`
  { label: '上传时间', prop: 'createTime', width: 160 },
  // [zh] TypeScript/JS 语句：`{ label: '操作', scopedSlots: 'slotOperation', width: 80 `
  { label: '操作', scopedSlots: 'slotOperation', width: 80 }
// [zh] TypeScript/JS 语句：`]`
]

// [zh] 声明变量 `tableInfoRef`
const tableInfoRef = ref()
// [zh] 声明变量 `handleRef`
const handleRef = ref()
// [zh] 声明变量 `searchForm`
const searchForm = reactive({
  // [zh] TypeScript/JS 语句：`userIdFuzzy: '',`
  userIdFuzzy: '',
  // [zh] TypeScript/JS 语句：`scene: '',`
  scene: '',
  // [zh] TypeScript/JS 语句：`status: 0`
  status: 0
// [zh] TypeScript/JS 语句：`})`
})
// [zh] 声明变量 `tableData`
const tableData = ref({})

// [zh] 声明变量 `imageUrl`
const imageUrl = (path) => `${proxy.Api.sourcePath}${encodeURIComponent(path)}`

// [zh] 声明变量 `loadDataList`
const loadDataList = async () => {
  // [zh] 声明变量 `params`
  const params = {
    // [zh] TypeScript/JS 语句：`pageNo: tableData.value.pageNo,`
    pageNo: tableData.value.pageNo,
    // [zh] TypeScript/JS 语句：`pageSize: tableData.value.pageSize`
    pageSize: tableData.value.pageSize
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (searchForm.userIdFuzzy) params.userIdFuzzy = searchForm.userIdFuzzy
  // [zh] 条件判断 if
  if (searchForm.scene) params.scene = searchForm.scene
  // [zh] 条件判断 if
  if (searchForm.status !== undefined && searchForm.status !== '') params.status = searchForm.status
  // [zh] 声明变量 `result`
  const result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.imageModerationLoadList,`
    url: proxy.Api.imageModerationLoadList,
    // [zh] TypeScript/JS 语句：`params`
    params
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) return
  // [zh] TypeScript/JS 语句：`Object.assign(tableData.value, result.data)`
  Object.assign(tableData.value, result.data)
// [zh] 语法符号
}

// [zh] 声明变量 `openHandle`
const openHandle = (row) => {
  // [zh] TypeScript/JS 语句：`handleRef.value?.show(row)`
  handleRef.value?.show(row)
// [zh] 语法符号
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.thumb {` */
.thumb {
  /* [zh] 样式规则 `width: 56px;` */
  width: 56px;
  /* [zh] 样式规则 `height: 56px;` */
  height: 56px;
  /* [zh] 样式规则 `object-fit: cover;` */
  object-fit: cover;
  /* [zh] 样式规则 `border-radius: 6px;` */
  border-radius: 6px;
  /* [zh] 样式规则 `background: #f0f0f0;` */
  background: #f0f0f0;
/* [zh] 样式规则 `}` */
}
</style>
