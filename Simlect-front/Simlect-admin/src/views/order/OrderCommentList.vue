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
          <el-form-item label="用户昵称">
            <!-- [zh] 开始标签 `<el-input>` -->
            <el-input clearable placeholder="输入用户昵称" v-model="searchForm.nickNameFuzzy"></el-input>
          <!-- [zh] 闭合标签 `</el-form-item>` -->
          </el-form-item>
        <!-- [zh] 闭合标签 `</el-col>` -->
        </el-col>
        <!-- [zh] 开始标签 `<el-col>` -->
        <el-col :span="5">
          <!-- [zh] 开始标签 `<el-form-item>` -->
          <el-form-item label="商品" prop="">
            <!-- [zh] 开始标签 `<el-input>` -->
            <el-input clearable placeholder="输入商品名称" v-model="searchForm.productNameFuzzy"></el-input>
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
        <template #slotUser="{ index, row }">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="user-panel">
            <!-- [zh] 开始标签 `<Avatar>` -->
            <Avatar :avatar="row.avatar" :width="50"></Avatar>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="nick-name">{{row.nickName}}</div>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotProduct="{ index, row }">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="product-panel">
            <!-- [zh] 开始标签 `<Cover>` -->
            <Cover :source="row.cover?.split(',')[0]" :width="100"></Cover>
            <!-- [zh] 开始标签 `<div>` -->
            <div class="product-name">
              <!-- [zh] Mustache 插值表达式 -->
              {{row.productName}}
              <!-- [zh] 开始标签 `<span>` -->
              <span v-if="row.orderItems && row.orderItems.length > 1" class="more-products-btn" @click.stop="showAllProducts(row)">等{{ row.orderItems.length }}件商品</span>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotCommentContent="{ index, row }">
          <!-- [zh] 开始标签 `<CommentDetail>` -->
          <CommentDetail :data="row" />
          <!-- [zh] 开始标签 `<div>` -->
          <div class="biz-comment" v-if="row.commentBizReply">商家回复：{{row.commentBizReply}}</div>
        </template>

        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template #slotOperation="{ index, row }">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="list-op-panel">
            <!-- [zh] 开始标签 `<OpBtn>` -->
            <OpBtn icon="icon-edit" tips="商家回复" @click="commentHandler(row.orderId)" />
            <!-- [zh] 开始标签 `<OpBtn>` -->
            <OpBtn icon="icon-delete" type="danger" tips="删除" @click="delComment(row)" />
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
        </template>
      </Table>
    </div>
  </el-card>
  <CommentReply ref="commentRef" @reload="loadDataList"></CommentReply>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `./CommentReply.vue`
import CommentReply from './CommentReply.vue'
// [zh] 导入 `./CommentDetail.vue`
import CommentDetail from './CommentDetail.vue'
// [zh] 导入 `模块`
import {
  // [zh] TypeScript/JS 语句：`ref,`
  ref,
  // [zh] TypeScript/JS 语句：`reactive,`
  reactive,
  // [zh] TypeScript/JS 语句：`getCurrentInstance,`
  getCurrentInstance,
  // [zh] TypeScript/JS 语句：`nextTick,`
  nextTick,
  // [zh] TypeScript/JS 语句：`createCommentVNode,`
  createCommentVNode,
// [zh] TypeScript/JS 语句：`} from 'vue'`
} from 'vue'
// [zh] 导入 `vue-router`
import { useRouter } from 'vue-router'
// [zh] 导入 `element-plus`
import { ElMessageBox } from 'element-plus'
// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `router`
const router = useRouter()

// [zh] 声明变量 `columns`
const columns = [
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '用户信息',`
    label: '用户信息',
    // [zh] TypeScript/JS 语句：`prop: 'user',`
    prop: 'user',
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotUser',`
    scopedSlots: 'slotUser',
    // [zh] TypeScript/JS 语句：`width: 150,`
    width: 150,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '商品信息',`
    label: '商品信息',
    // [zh] TypeScript/JS 语句：`prop: 'product',`
    prop: 'product',
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotProduct',`
    scopedSlots: 'slotProduct',
    // [zh] TypeScript/JS 语句：`width: 500,`
    width: 500,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '评论内容',`
    label: '评论内容',
    // [zh] TypeScript/JS 语句：`prop: 'commentContent',`
    prop: 'commentContent',
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotCommentContent',`
    scopedSlots: 'slotCommentContent',
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  {
    // [zh] TypeScript/JS 语句：`label: '操作',`
    label: '操作',
    // [zh] TypeScript/JS 语句：`prop: 'operation',`
    prop: 'operation',
    // [zh] TypeScript/JS 语句：`width: 120,`
    width: 120,
    // [zh] TypeScript/JS 语句：`scopedSlots: 'slotOperation',`
    scopedSlots: 'slotOperation',
  // [zh] TypeScript/JS 语句：`},`
  },
// [zh] TypeScript/JS 语句：`]`
]

// [zh] 声明变量 `tableInfoRef`
const tableInfoRef = ref()
// [zh] 声明变量 `searchForm`
const searchForm = ref({})
// [zh] 声明变量 `tableData`
const tableData = ref({})
// [zh] 声明变量 `loadDataList`
const loadDataList = async () => {
  // [zh] 声明变量 `params`
  let params = {
    // [zh] TypeScript/JS 语句：`pageNo: tableData.value.pageNo,`
    pageNo: tableData.value.pageNo,
    // [zh] TypeScript/JS 语句：`pageSize: tableData.value.pageSize,`
    pageSize: tableData.value.pageSize,
  // [zh] 语法符号
  }
  // [zh] TypeScript/JS 语句：`Object.assign(params, searchForm.value)`
  Object.assign(params, searchForm.value)
  // [zh] 声明变量 `result`
  let result = await proxy.Request({
    // [zh] TypeScript/JS 语句：`url: proxy.Api.loadComment,`
    url: proxy.Api.loadComment,
    // [zh] TypeScript/JS 语句：`params: params,`
    params: params,
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] 条件判断 if
  if (!result) {
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }

  // [zh] 代码块开始
  (result.data.list || []).forEach((item) => {
    // [zh] TypeScript/JS 语句：`item.commentImages = item.commentImages ? item.commentI`
    item.commentImages = item.commentImages ? item.commentImages.split(',') : []
    // [zh] TypeScript/JS 语句：`item.recommentImages = item.recommentImages`
    item.recommentImages = item.recommentImages
      // [zh] TypeScript/JS 语句：`? item.recommentImages.split(',')`
      ? item.recommentImages.split(',')
      // [zh] TypeScript/JS 语句：`: []`
      : []
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] TypeScript/JS 语句：`Object.assign(tableData.value, result.data)`
  Object.assign(tableData.value, result.data)
// [zh] 语法符号
}

// [zh] 声明变量 `delComment`
const delComment = (row) => {
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: `确定要删除评论吗？`,`
    message: `确定要删除评论吗？`,
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      let result = await proxy.Request({
        // [zh] TypeScript/JS 语句：`url: proxy.Api.delComment,`
        url: proxy.Api.delComment,
        // [zh] 代码块开始
        params: {
          // [zh] TypeScript/JS 语句：`orderId: row.orderId,`
          orderId: row.orderId,
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
      // [zh] TypeScript/JS 语句：`proxy.Message.success('操作成功')`
      proxy.Message.success('操作成功')
      // [zh] TypeScript/JS 语句：`loadDataList()`
      loadDataList()
    // [zh] TypeScript/JS 语句：`},`
    },
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 声明变量 `showAllProducts`
const showAllProducts = (row) => {
  // [zh] 声明变量 `items`
  const items = row.orderItems || []
  // [zh] 声明变量 `html`
  let html = '<div style="max-height:400px;overflow-y:auto;">'
  // [zh] 代码块开始
  items.forEach((item, idx) => {
    // [zh] 声明变量 `cover`
    const cover = item.cover ? `<img src="${proxy.AppConfig.imageRootUrl}${item.cover.split(',')[0]}" style="width:60px;height:60px;object-fit:cover;border-radius:6px;flex-shrink:0;" />` : ''
    // [zh] TypeScript/JS 语句：`html += `<div style="display:flex;gap:12px;padding:10px`
    html += `<div style="display:flex;gap:12px;padding:10px 0;${idx > 0 ? 'border-top:1px solid #eee;' : ''}">
      // [zh] TypeScript/JS 语句：`${cover}`
      ${cover}
      // [zh] TypeScript/JS 语句：`<div style="flex:1;min-width:0;">`
      <div style="flex:1;min-width:0;">
        // [zh] TypeScript/JS 语句：`<div style="font-size:14px;font-weight:500;margin-botto`
        <div style="font-size:14px;font-weight:500;margin-bottom:4px;color:#1d1d1f;">${item.productName || ''}</div>
        // [zh] TypeScript/JS 语句：`<div style="font-size:12px;color:#86868b;">${item.prope`
        <div style="font-size:12px;color:#86868b;">${item.propertyInfo || ''}</div>
        // [zh] TypeScript/JS 语句：`<div style="font-size:12px;color:#86868b;margin-top:2px`
        <div style="font-size:12px;color:#86868b;margin-top:2px;">￥${item.itemAmount || 0} × ${item.buyCount || 0}</div>
      // [zh] TypeScript/JS 语句：`</div>`
      </div>
    // [zh] TypeScript/JS 语句：`</div>``
    </div>`
  // [zh] TypeScript/JS 语句：`})`
  })
  // [zh] TypeScript/JS 语句：`html += '</div>'`
  html += '</div>'
  // [zh] 代码块开始
  try {
    // [zh] 代码块开始
    ElMessageBox.alert(html, '该订单商品', {
      // [zh] TypeScript/JS 语句：`dangerouslyUseHTMLString: true,`
      dangerouslyUseHTMLString: true,
      // [zh] TypeScript/JS 语句：`confirmButtonText: '关闭',`
      confirmButtonText: '关闭',
      // [zh] TypeScript/JS 语句：`showCancelButton: false,`
      showCancelButton: false,
      // [zh] TypeScript/JS 语句：`closeOnClickModal: true,`
      closeOnClickModal: true,
    // [zh] TypeScript/JS 语句：`})`
    })
  // [zh] 代码块开始
  } catch (e) {
    // [zh] TypeScript/JS 语句：`console.error(e)`
    console.error(e)
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 声明变量 `commentRef`
const commentRef = ref()
// [zh] 声明变量 `commentHandler`
const commentHandler = (orderId) => {
  // [zh] TypeScript/JS 语句：`commentRef.value.show(orderId)`
  commentRef.value.show(orderId)
// [zh] 语法符号
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.table-panel {` */
.table-panel {
  /* [zh] 样式规则 `height: calc(100vh - 135px);` */
  height: calc(100vh - 135px);

  /* [zh] 样式规则 `.user-panel {` */
  .user-panel {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `.nick-name {` */
    .nick-name {
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
      /* [zh] 样式规则 `margin-left: 5px;` */
      margin-left: 5px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.product-panel {` */
  .product-panel {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `.product-name {` */
    .product-name {
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
      /* [zh] 样式规则 `margin-left: 5px;` */
      margin-left: 5px;
    /* [zh] 样式规则 `}` */
    }
    /* [zh] 样式规则 `.more-products-btn {` */
    .more-products-btn {
      /* [zh] 样式规则 `display: inline-block;` */
      display: inline-block;
      /* [zh] 样式规则 `margin-left: 6px;` */
      margin-left: 6px;
      /* [zh] 样式规则 `padding: 2px 8px;` */
      padding: 2px 8px;
      /* [zh] 样式规则 `border-radius: 4px;` */
      border-radius: 4px;
      /* [zh] 样式规则 `background: #000;` */
      background: #000;
      /* [zh] 样式规则 `color: #fff;` */
      color: #fff;
      /* [zh] 样式规则 `font-size: 11px;` */
      font-size: 11px;
      /* [zh] 样式规则 `cursor: pointer;` */
      cursor: pointer;
      /* [zh] 样式规则 `white-space: nowrap;` */
      white-space: nowrap;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.biz-comment {` */
  .biz-comment {
    /* [zh] 样式规则 `color: var(--pink);` */
    color: var(--pink);
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
