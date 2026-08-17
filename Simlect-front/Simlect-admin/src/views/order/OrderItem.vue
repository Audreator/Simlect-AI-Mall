<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="order-item">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="order-title">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="order-base">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="order-time">{{ data.orderTime }}</div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="order-id">订单号: {{ data.orderId }}</div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="buyer">买家信息:{{ data.userId }} / {{ data.nickName }}</div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
      <!-- [zh] 开始标签 `<div>` -->
      <div class="order-status">{{ data.orderStatusName }}</div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<div>` -->
    <div class="sub-item-list">
      <!-- [zh] 开始标签 `<div>` -->
      <div class="order-sub-item" v-for="(sub, index) in data.orderItemList">
        <!-- [zh] 开始标签 `<CouponOrderCover>` -->
        <CouponOrderCover v-if="isCouponItem(sub)" :width="80" />
        <!-- [zh] 开始标签 `<Cover>` -->
        <Cover v-else :source="sub.cover" :width="80"></Cover>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="product-name-panel">
          <!-- [zh] 开始标签 `<div>` -->
          <div class="product-name">{{ sub.productName }}</div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="property">{{ sub.propertyInfo }}</div>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="remark">买家备注:{{ sub.remark || '暂无' }}</div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="amount-info">
          <!-- [zh] 开始标签 `<Price>` -->
          <Price :price="sub.itemAmount"></Price>
          <!-- [zh] 开始标签 `<div>` -->
          <div class="buy-count">x{{ sub.buyCount }}</div>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="total-amount">
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template v-if="index == 0">
            <!-- [zh] 开始标签 `<div>` -->
            <div class="amount-detail">
              <!-- [zh] 开始标签 `<div>` -->
              <div v-if="hasCouponDiscount" class="amount-line muted">
                <!-- [zh] 开始标签 `<span>` -->
                <span>商品总价</span>
                <!-- [zh] 开始标签 `<Price>` -->
                <Price :price="data.originalAmount"></Price>
              <!-- [zh] 闭合标签 `</div>` -->
              </div>
              <!-- [zh] 开始标签 `<div>` -->
              <div v-if="hasCouponDiscount" class="amount-line discount">
                <!-- [zh] 开始标签 `<span>` -->
                <span>优惠券{{ couponLabel ? `：${couponLabel}` : '' }}</span>
                <!-- [zh] 开始标签 `<span>` -->
                <span>-¥{{ formatDiscount(data.couponDiscountAmount) }}</span>
              <!-- [zh] 闭合标签 `</div>` -->
              </div>
              <!-- [zh] 开始标签 `<div>` -->
              <div class="amount-line pay">
                <!-- [zh] 开始标签 `<span>` -->
                <span class="total-amount-tips">实付款</span>
                <!-- [zh] 开始标签 `<Price>` -->
                <Price :price="data.amount"></Price>
              <!-- [zh] 闭合标签 `</div>` -->
              </div>
            <!-- [zh] 闭合标签 `</div>` -->
            </div>
          </template>
        </div>
        <div class="order-op-panel">
          <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
          <template v-if="index == 0">
            <!-- [zh] 开始标签 `<el-button>` -->
            <el-button type="primary" v-if="data.orderStatus == 1" class="btn" @click="delivery">确认发货</el-button>
            <!-- [zh] 开始标签 `<el-button>` -->
            <el-button type="primary" v-if="data.commentStatus != 0" @click="comment">回复买家</el-button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { isCouponOrder, isCouponOrderItem } from '@/utils/order.js'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const props = defineProps({
  data: {
    type: Object,
    default: {},
  },
})

const isCouponItem = (item) => isCouponOrder(props.data) || isCouponOrderItem(item)

const hasCouponDiscount = computed(() => Number(props.data?.couponDiscountAmount ?? 0) > 0)

const couponLabel = computed(() => {
  const name = props.data?.couponName
  if (!name) return ''
  const typeMap = { 1: '满减券', 2: '折扣券', 3: '无门槛券' }
  const type = typeMap[props.data?.couponType]
  return type ? `${name}（${type}）` : String(name)
})

const formatDiscount = (val) => Number(val ?? 0).toFixed(2)

const totalAmount = computed(() => {
  return props.data.orderItemList.reduce((total, item) => {
    return total + item.amount
  }, 0)
})

const emit = defineEmits(['delivery', 'comment'])
const delivery = () => {
  emit('delivery', props.data)
}

const commentRef = ref()
const comment = () => {
  emit('comment', props.data.orderId)
}
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.order-item {` */
.order-item {
  /* [zh] 样式规则 `.order-title {` */
  .order-title {
    /* [zh] 样式规则 `background: #ebebeb;` */
    background: #ebebeb;
    /* [zh] 样式规则 `padding: 10px;` */
    padding: 10px;
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `border-radius: 5px;` */
    border-radius: 5px;
    /* [zh] 样式规则 `color: var(--text);` */
    color: var(--text);

    /* [zh] 样式规则 `.order-base {` */
    .order-base {
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
      /* [zh] 样式规则 `width: 0;` */
      width: 0;

      /* [zh] 样式规则 `.order-id {` */
      .order-id {
        /* [zh] 样式规则 `margin-left: 20px;` */
        margin-left: 20px;
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.buyer {` */
      .buyer {
        /* [zh] 样式规则 `margin-left: 20px;` */
        margin-left: 20px;
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.order-status {` */
    .order-status {
      /* [zh] 样式规则 `color: var(--red);` */
      color: var(--red);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.sub-item-list {` */
  .sub-item-list {
    /* [zh] 样式规则 `.order-sub-item {` */
    .order-sub-item {
      /* [zh] 样式规则 `display: flex;` */
      display: flex;
      /* [zh] 样式规则 `margin: 10px 0px 0px;` */
      margin: 10px 0px 0px;
      /* [zh] 样式规则 `align-items: flex-start;` */
      align-items: flex-start;

      /* [zh] 样式规则 `.product-name-panel {` */
      .product-name-panel {
        /* [zh] 样式规则 `flex: 1;` */
        flex: 1;
        /* [zh] 样式规则 `width: 0;` */
        width: 0;
        /* [zh] 样式规则 `margin: 0px 10px;` */
        margin: 0px 10px;
        /* [zh] 样式规则 `color: var(--text);` */
        color: var(--text);

        /* [zh] 样式规则 `.product-name {` */
        .product-name {
          /* [zh] 样式规则 `cursor: pointer;` */
          cursor: pointer;

          /* [zh] 样式规则 `&:hover {` */
          &:hover {
            /* [zh] 样式规则 `color: var(--red);` */
            color: var(--red);
          /* [zh] 样式规则 `}` */
          }
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `.property {` */
        .property {
          /* [zh] 样式规则 `margin-top: 5px;` */
          margin-top: 5px;
          /* [zh] 样式规则 `font-size: 12px;` */
          font-size: 12px;
          /* [zh] 样式规则 `color: var(--text2);` */
          color: var(--text2);
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `.remark {` */
        .remark {
          /* [zh] 样式规则 `margin-top: 5px;` */
          margin-top: 5px;
          /* [zh] 样式规则 `font-size: 12px;` */
          font-size: 12px;
          /* [zh] 样式规则 `color: #000;` */
          color: #000;
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.amount-info {` */
      .amount-info {
        /* [zh] 样式规则 `width: 100px;` */
        width: 100px;
        /* [zh] 样式规则 `display: flex;` */
        display: flex;
        /* [zh] 样式规则 `flex-direction: column;` */
        flex-direction: column;
        /* [zh] 样式规则 `align-items: flex-end;` */
        align-items: flex-end;

        /* [zh] 样式规则 `.buy-count {` */
        .buy-count {
          /* [zh] 样式规则 `font-size: 12px;` */
          font-size: 12px;
          /* [zh] 样式规则 `color: var(--text2);` */
          color: var(--text2);
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.total-amount {` */
      .total-amount {
        /* [zh] 样式规则 `width: 220px;` */
        width: 220px;
        /* [zh] 样式规则 `display: flex;` */
        display: flex;
        /* [zh] 样式规则 `align-items: flex-end;` */
        align-items: flex-end;
        /* [zh] 样式规则 `justify-content: flex-end;` */
        justify-content: flex-end;
        /* [zh] 样式规则 `font-size: 12px;` */
        font-size: 12px;

        /* [zh] 样式规则 `.amount-detail {` */
        .amount-detail {
          /* [zh] 样式规则 `display: flex;` */
          display: flex;
          /* [zh] 样式规则 `flex-direction: column;` */
          flex-direction: column;
          /* [zh] 样式规则 `align-items: flex-end;` */
          align-items: flex-end;
          /* [zh] 样式规则 `gap: 4px;` */
          gap: 4px;
          /* [zh] 样式规则 `width: 100%;` */
          width: 100%;
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `.amount-line {` */
        .amount-line {
          /* [zh] 样式规则 `display: flex;` */
          display: flex;
          /* [zh] 样式规则 `align-items: baseline;` */
          align-items: baseline;
          /* [zh] 样式规则 `justify-content: flex-end;` */
          justify-content: flex-end;
          /* [zh] 样式规则 `gap: 6px;` */
          gap: 6px;
          /* [zh] 样式规则 `width: 100%;` */
          width: 100%;

          /* [zh] 样式规则 `&.muted {` */
          &.muted {
            /* [zh] 样式规则 `color: var(--text2);` */
            color: var(--text2);
          /* [zh] 样式规则 `}` */
          }

          /* [zh] 样式规则 `&.discount {` */
          &.discount {
            /* [zh] 样式规则 `color: var(--red);` */
            color: var(--red);
          /* [zh] 样式规则 `}` */
          }

          /* [zh] 样式规则 `&.pay {` */
          &.pay {
            /* [zh] 样式规则 `margin-top: 2px;` */
            margin-top: 2px;
          /* [zh] 样式规则 `}` */
          }
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `.total-amount-tips {` */
        .total-amount-tips {
          /* [zh] 样式规则 `margin-right: 3px;` */
          margin-right: 3px;
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }

      /* [zh] 样式规则 `.order-op-panel {` */
      .order-op-panel {
        /* [zh] 样式规则 `margin-left: 10px;` */
        margin-left: 10px;
        /* [zh] 样式规则 `width: 110px;` */
        width: 110px;
        /* [zh] 样式规则 `display: flex;` */
        display: flex;
        /* [zh] 样式规则 `flex-direction: column;` */
        flex-direction: column;
        /* [zh] 样式规则 `align-items: center;` */
        align-items: center;
        /* [zh] 样式规则 `justify-content: center;` */
        justify-content: center;

        /* [zh] 样式规则 `.btn {` */
        .btn {
          /* [zh] 样式规则 `width: 90px;` */
          width: 90px;
          /* [zh] 样式规则 `margin-bottom: 10px;` */
          margin-bottom: 10px;
        /* [zh] 样式规则 `}` */
        }

        /* [zh] 样式规则 `.btn-link {` */
        .btn-link {
          /* [zh] 样式规则 `cursor: pointer;` */
          cursor: pointer;
        /* [zh] 样式规则 `}` */
        }
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
