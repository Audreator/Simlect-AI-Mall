<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="order-logistics-page card" v-loading="loading">
    <!-- [zh] 开始标签 `<section>` -->
    <section v-if="logistics" class="logistics-section">
      <!-- [zh] 开始标签 `<h3>` -->
      <h3 class="section-title">物流信息</h3>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="logistics-head">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="company-line">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="company">{{ logistics.logisticsCompany || '物流公司' }}</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="no">{{ logistics.logisticsNo }}</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<el-tag>` -->
        <el-tag type="info" effect="plain" size="small">{{ logistics.logisticsStatusName }}</el-tag>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<div>` -->
      <div class="address-card">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="addr-row">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="label">发件</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="text">
            <!-- [zh] Mustache 插值表达式 -->
            {{ logistics.senderName }} {{ logistics.senderPhone }}
            <!-- [zh] 开始标签 `<br>` -->
            <br />
            <!-- [zh] Mustache 插值表达式 -->
            {{ logistics.senderAddress }}
          <!-- [zh] 闭合标签 `</span>` -->
          </span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<div>` -->
        <div class="addr-row">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="label">收件</span>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="text">
            <!-- [zh] Mustache 插值表达式 -->
            {{ logistics.receiverName }} {{ logistics.receiverPhone }}
            <!-- [zh] 开始标签 `<br>` -->
            <br />
            <!-- [zh] Mustache 插值表达式 -->
            {{ logistics.receiverAddress }}
          <!-- [zh] 闭合标签 `</span>` -->
          </span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>

      <!-- [zh] 开始标签 `<el-timeline>` -->
      <el-timeline v-if="recordList.length" class="track-timeline">
        <!-- [zh] 开始标签 `<el-timeline-item>` -->
        <el-timeline-item
          v-for="(item, index) in recordList"
          :key="item.recordId"
          placement="top"
          :hide-timestamp="true"
        >
          <!-- [zh] 开始标签 `<div>` -->
          <div class="track-head">
            <!-- [zh] 开始标签 `<span>` -->
            <span v-if="index === 0" class="status-name">{{ logistics.logisticsStatusName }}</span>
            <!-- [zh] 开始标签 `<span>` -->
            <span class="track-time">{{ item.recordTime }}</span>
          <!-- [zh] 闭合标签 `</div>` -->
          </div>
          <!-- [zh] 开始标签 `<p>` -->
          <p class="track-address">{{ item.recordAddress }}</p>
        <!-- [zh] 闭合标签 `</el-timeline-item>` -->
        </el-timeline-item>
      <!-- [zh] 闭合标签 `</el-timeline>` -->
      </el-timeline>
    <!-- [zh] 闭合标签 `</section>` -->
    </section>

    <!-- [zh] 开始标签 `<el-empty>` -->
    <el-empty v-else-if="!loading" description="暂无物流信息" />
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { orderApi } from '@/api/modules';

const route = useRoute();
const loading = ref(true);
const logistics = ref<Record<string, any> | null>(null);

const recordList = computed(() => {
  const list = logistics.value?.recordList;
  return Array.isArray(list) ? list : [];
});

const load = async () => {
  loading.value = true;
  try {
    logistics.value = (await orderApi.getLogistics(String(route.params.orderId))) || null;
  } finally {
    loading.value = false;
  }
};

onMounted(load);
</script>

/* [zh] 样式声明 */
<style scoped lang="scss">
/* [zh] 样式规则 `@use '@/styles/variables' as *;` */
@use '@/styles/variables' as *;

/* [zh] 样式规则 `.order-logistics-page {` */
.order-logistics-page {
  /* [zh] 样式规则 `padding: 14px 16px;` */
  padding: 14px 16px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.section-title {` */
.section-title {
  /* [zh] 样式规则 `margin: 0 0 12px;` */
  margin: 0 0 12px;
  /* [zh] 样式规则 `font-size: 16px;` */
  font-size: 16px;
  /* [zh] 样式规则 `font-weight: 600;` */
  font-weight: 600;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.logistics-head {` */
.logistics-head {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: space-between;` */
  justify-content: space-between;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
  /* [zh] 样式规则 `margin-bottom: 12px;` */
  margin-bottom: 12px;
  /* [zh] 样式规则 `padding-bottom: 12px;` */
  padding-bottom: 12px;
  /* [zh] 样式规则 `border-bottom: 1px solid $color-border;` */
  border-bottom: 1px solid $color-border;

  /* [zh] 样式规则 `.company-line {` */
  .company-line {
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `color: $color-text-title;` */
    color: $color-text-title;

    /* [zh] 样式规则 `.company {` */
    .company {
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
      /* [zh] 样式规则 `margin-right: 8px;` */
      margin-right: 8px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.no {` */
    .no {
      /* [zh] 样式规则 `color: $color-text-body;` */
      color: $color-text-body;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.address-card {` */
.address-card {
  /* [zh] 样式规则 `margin-bottom: 16px;` */
  margin-bottom: 16px;
  /* [zh] 样式规则 `padding: 10px 12px;` */
  padding: 10px 12px;
  /* [zh] 样式规则 `background: #fafafa;` */
  background: #fafafa;
  /* [zh] 样式规则 `border-radius: $radius-xs;` */
  border-radius: $radius-xs;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `line-height: 1.5;` */
  line-height: 1.5;

  /* [zh] 样式规则 `.addr-row {` */
  .addr-row {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;

    /* [zh] 样式规则 `& + .addr-row {` */
    & + .addr-row {
      /* [zh] 样式规则 `margin-top: 8px;` */
      margin-top: 8px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.label {` */
    .label {
      /* [zh] 样式规则 `flex-shrink: 0;` */
      flex-shrink: 0;
      /* [zh] 样式规则 `color: $color-text-muted;` */
      color: $color-text-muted;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.text {` */
    .text {
      /* [zh] 样式规则 `color: $color-text-body;` */
      color: $color-text-body;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.track-timeline {` */
.track-timeline {
  /* [zh] 样式规则 `padding-left: 4px;` */
  padding-left: 4px;

  /* [zh] 样式规则 `.track-head {` */
  .track-head {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-wrap: wrap;` */
    flex-wrap: wrap;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;

    /* [zh] 样式规则 `.status-name {` */
    .status-name {
      /* [zh] 样式规则 `color: $color-primary;` */
      color: $color-primary;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.track-time {` */
    .track-time {
      /* [zh] 样式规则 `color: $color-text-muted;` */
      color: $color-text-muted;
      /* [zh] 样式规则 `font-size: 12px;` */
      font-size: 12px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.track-address {` */
  .track-address {
    /* [zh] 样式规则 `margin: 6px 0 0;` */
    margin: 6px 0 0;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: $color-text-body;` */
    color: $color-text-body;
    /* [zh] 样式规则 `line-height: 1.5;` */
    line-height: 1.5;
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}
</style>
