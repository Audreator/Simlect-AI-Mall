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

<script setup>
import CommentReply from './CommentReply.vue'
import CommentDetail from './CommentDetail.vue'
import {
  ref,
  reactive,
  getCurrentInstance,
  nextTick,
  createCommentVNode,
} from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
const { proxy } = getCurrentInstance()
const router = useRouter()

const columns = [
  {
    label: '用户信息',
    prop: 'user',
    scopedSlots: 'slotUser',
    width: 150,
  },
  {
    label: '商品信息',
    prop: 'product',
    scopedSlots: 'slotProduct',
    width: 500,
  },
  {
    label: '评论内容',
    prop: 'commentContent',
    scopedSlots: 'slotCommentContent',
  },
  {
    label: '操作',
    prop: 'operation',
    width: 120,
    scopedSlots: 'slotOperation',
  },
]

const tableInfoRef = ref()
const searchForm = ref({})
const tableData = ref({})
const loadDataList = async () => {
  let params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
  }
  Object.assign(params, searchForm.value)
  let result = await proxy.Request({
    url: proxy.Api.loadComment,
    params: params,
  })
  if (!result) {
    return
  }

  (result.data.list || []).forEach((item) => {
    item.commentImages = item.commentImages ? item.commentImages.split(',') : []
    item.recommentImages = item.recommentImages
      ? item.recommentImages.split(',')
      : []
  })
  Object.assign(tableData.value, result.data)
}

const delComment = (row) => {
  proxy.Confirm({
    message: `确定要删除评论吗？`,
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.delComment,
        params: {
          orderId: row.orderId,
        },
      })
      if (!result) {
        return
      }
      proxy.Message.success('操作成功')
      loadDataList()
    },
  })
}

const showAllProducts = (row) => {
  const items = row.orderItems || []
  let html = '<div style="max-height:400px;overflow-y:auto;">'
  items.forEach((item, idx) => {
    const cover = item.cover ? `<img src="${proxy.AppConfig.imageRootUrl}${item.cover.split(',')[0]}" style="width:60px;height:60px;object-fit:cover;border-radius:6px;flex-shrink:0;" />` : ''
    html += `<div style="display:flex;gap:12px;padding:10px 0;${idx > 0 ? 'border-top:1px solid #eee;' : ''}">
      ${cover}
      <div style="flex:1;min-width:0;">
        <div style="font-size:14px;font-weight:500;margin-bottom:4px;color:#1d1d1f;">${item.productName || ''}</div>
        <div style="font-size:12px;color:#86868b;">${item.propertyInfo || ''}</div>
        <div style="font-size:12px;color:#86868b;margin-top:2px;">￥${item.itemAmount || 0} × ${item.buyCount || 0}</div>
      </div>
    </div>`
  })
  html += '</div>'
  try {
    ElMessageBox.alert(html, '该订单商品', {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '关闭',
      showCancelButton: false,
      closeOnClickModal: true,
    })
  } catch (e) {
    console.error(e)
  }
}

const commentRef = ref()
const commentHandler = (orderId) => {
  commentRef.value.show(orderId)
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
