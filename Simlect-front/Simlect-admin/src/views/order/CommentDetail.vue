<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>

  <!-- [zh] 开始标签 `<div>` -->
  <div class="comment-panel">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="comment-title">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="title">初次评价</div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="commend-time">{{data.commentTime}}</div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="del" v-if="data.status==1">已删除</div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="comment-inner">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="comment-info">{{ data.commentContent }}</div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="comment-images" v-if="data.commentImages?.length > 0">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="comment-image-item" v-for="(item, index) in data.commentImages">
          <!-- [zh] 开始标签 `<Cover>` -->
          <Cover fit="cover" :source="item" :preImageList="data.commentImages"></Cover>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<el-rate>` -->
      <el-rate v-model="data.star" size="large" disabled />
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="comment-panel" v-if="data.recommentContent">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="comment-title">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="title">追评</div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="commend-time">{{data.recommentTime}}</div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="comment-inner">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="comment-info">{{ data.recommentContent }}</div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="comment-images" v-if="data.recommentImages?.length > 0">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="comment-image-item" v-for="(item, index) in data.recommentImages">
          <!-- [zh] 开始标签 `<Cover>` -->
          <Cover fit="cover" :source="item" :preImageList="data.recommentImages"></Cover>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const props = defineProps({
  data: {
    type: Object,
    default: {},
  },
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.comment-panel {` */
.comment-panel {
  /* [zh] 样式规则 `margin-bottom: 10px;` */
  margin-bottom: 10px;
  /* [zh] 样式规则 `border: 1px solid #ddd;` */
  border: 1px solid #ddd;
  /* [zh] 样式规则 `border-radius: 5px;` */
  border-radius: 5px;

  /* [zh] 样式规则 `.comment-title {` */
  .comment-title {
    /* [zh] 样式规则 `background: #ebebeb;` */
    background: #ebebeb;
    /* [zh] 样式规则 `padding: 5px;` */
    padding: 5px;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `.title {` */
    .title {
      /* [zh] 样式规则 `font-size: 13px;` */
      font-size: 13px;
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
    /* [zh] 样式规则 `}` */
    }
    /* [zh] 样式规则 `.commend-time {` */
    .commend-time {
      /* [zh] 样式规则 `font-size: 13px;` */
      font-size: 13px;
      /* [zh] 样式规则 `margin-bottom: 3px;` */
      margin-bottom: 3px;
      /* [zh] 样式规则 `color: var(--text2);` */
      color: var(--text2);
    /* [zh] 样式规则 `}` */
    }
    /* [zh] 样式规则 `.del {` */
    .del {
      /* [zh] 样式规则 `margin-left: 10px;` */
      margin-left: 10px;
      /* [zh] 样式规则 `color: red;` */
      color: red;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.comment-inner {` */
  .comment-inner {
    /* [zh] 样式规则 `padding: 10px;` */
    padding: 10px;
    /* [zh] 样式规则 `.comment-info {` */
    .comment-info {
      /* [zh] 样式规则 `margin-bottom: 5px;` */
      margin-bottom: 5px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.comment-images {` */
.comment-images {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `margin-top: 5px;` */
  margin-top: 5px;

  /* [zh] 样式规则 `.comment-image-item {` */
  .comment-image-item {
    /* [zh] 样式规则 `margin-top: 5px;` */
    margin-top: 5px;
    /* [zh] 样式规则 `width: 72px;` */
    width: 72px;
    /* [zh] 样式规则 `height: 72px;` */
    height: 72px;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `position: relative;` */
    position: relative;
    /* [zh] 样式规则 `margin-right: 5px;` */
    margin-right: 5px;

    /* [zh] 样式规则 `.del {` */
    .del {
      /* [zh] 样式规则 `position: absolute;` */
      position: absolute;
      /* [zh] 样式规则 `top: 0px;` */
      top: 0px;
      /* [zh] 样式规则 `right: 0px;` */
      right: 0px;
      /* [zh] 样式规则 `width: 20px;` */
      width: 20px;
      /* [zh] 样式规则 `height: 20px;` */
      height: 20px;
      /* [zh] 样式规则 `color: #fff;` */
      color: #fff;
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `align-items: center;` */
      align-items: center;
      /* [zh] 样式规则 `justify-content: center;` */
      justify-content: center;
      /* [zh] 样式规则 `border-radius: 0px 5px 0px 5px;` */
      border-radius: 0px 5px 0px 5px;
      /* [zh] 样式规则 `cursor: pointer;` */
      cursor: pointer;
      /* [zh] 样式规则 `color: #fff;` */
      color: #fff;
      /* [zh] 样式规则 `background: var(--red);` */
      background: var(--red);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
