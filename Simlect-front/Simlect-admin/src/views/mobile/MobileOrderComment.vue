<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="m-simple">
    <!-- [zh] 开始标签 `<div>` -->
    <div class="m-search glass-card glass-strong">
      <!-- [zh] 开始标签 `<input>` -->
      <input v-model="searchForm.nickNameFuzzy" class="search-input" placeholder="用户昵称" @keyup.enter="reload" />
      <!-- [zh] 开始标签 `<input>` -->
      <input v-model="searchForm.productNameFuzzy" class="search-input" placeholder="商品名称" @keyup.enter="reload" />
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<div>` -->
    <div v-if="list.length" class="m-list">
      <!-- [zh] 开始标签 `<div>` -->
      <div v-for="row in list" :key="row.orderId" class="glass-card cmt-card" :class="{ 'cmt-deleted': row.status == 1 }">
        <!-- [zh] 开始标签 `<div>` -->
        <div class="cmt-head">
          <!-- [zh] 开始标签 `<Avatar>` -->
          <Avatar :avatar="row.avatar || undefined" :width="36"></Avatar>
          <!-- [zh] 开始标签 `<span>` -->
          <span class="cmt-nick">{{ row.nickName }}</span>
          <!-- [zh] 开始标签 `<el-rate>` -->
          <el-rate v-if="row.star" :model-value="row.star" disabled size="small" />
          <!-- [zh] 开始标签 `<span>` -->
          <span v-if="row.status == 1" class="deleted-badge">已删除</span>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="cmt-product">
          <!-- [zh] Mustache 插值表达式 -->
          {{ row.productName }}
          <!-- [zh] 开始标签 `<span>` -->
          <span v-if="row.orderItems && row.orderItems.length > 1" class="more-products-btn" @click.stop="showAllProducts(row)">等{{ row.orderItems.length }}件商品</span>
        <!-- [zh] 闭合标签 `</p>` -->
        </p>
        <!-- [zh] 开始标签 `<p>` -->
        <p class="cmt-content">{{ row.commentContent }}</p>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="row.commentImages.length" class="cmt-imgs">
          <!-- [zh] 开始标签 `<Cover>` -->
          <Cover v-for="(img, i) in row.commentImages" :key="i" :source="img" :width="56" border-radius="8px"></Cover>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<p>` -->
        <p v-if="row.recommentContent" class="cmt-recomment">
          <!-- [zh] 开始标签 `<span>` -->
          <span class="tag">追评</span>{{ row.recommentContent }}
        <!-- [zh] 闭合标签 `</p>` -->
        </p>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="row.recommentImages.length" class="cmt-imgs">
          <!-- [zh] 开始标签 `<Cover>` -->
          <Cover v-for="(img, i) in row.recommentImages" :key="i" :source="img" :width="56" border-radius="8px"></Cover>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
        <!-- [zh] 开始标签 `<p>` -->
        <p v-if="row.commentBizReply" class="cmt-reply"><span class="tag">商家</span>{{ row.commentBizReply }}</p>
        <!-- [zh] 开始标签 `<div>` -->
        <div v-if="row.status != 1" class="cmt-ops">
          <!-- [zh] 开始标签 `<button>` -->
          <button type="button" class="op-btn" @click="reply(row)">商家回复</button>
          <!-- [zh] 开始标签 `<button>` -->
          <button type="button" class="op-btn danger" @click="del(row)">删除</button>
        <!-- [zh] 闭合标签 `</div>` -->
        </div>
      <!-- [zh] 闭合标签 `</div>` -->
      </div>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>
    <!-- [zh] 开始标签 `<p>` -->
    <p v-else-if="!loading" class="m-empty-tip">暂无评价</p>

    <!-- [zh] 开始标签 `<div>` -->
    <div ref="sentinel" class="m-sentinel">
      <!-- [zh] 开始标签 `<span>` -->
      <span v-if="loading">加载中…</span>
      <!-- [zh] 开始标签 `<span>` -->
      <span v-else-if="finished && list.length">没有更多了</span>
    <!-- [zh] 闭合标签 `</div>` -->
    </div>

    <!-- [zh] 开始标签 `<CommentReply>` -->
    <CommentReply ref="commentRef" @reload="reload"></CommentReply>
  <!-- [zh] 闭合标签 `</div>` -->
  </div>
</template>

// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `@/views/order/CommentReply.vue`
import CommentReply from '@/views/order/CommentReply.vue'
// [zh] 导入 `vue`
import { ref, reactive, getCurrentInstance, onMounted, onUnmounted } from 'vue'
// [zh] 导入 `element-plus`
import { ElMessageBox } from 'element-plus'

// [zh] TypeScript/JS 语句：`const { proxy } = getCurrentInstance()`
const { proxy } = getCurrentInstance()
// [zh] 声明变量 `searchForm`
const searchForm = reactive({ nickNameFuzzy: '', productNameFuzzy: '' })
// [zh] 声明变量 `list`
const list = ref([])
// [zh] 声明变量 `pageNo`
const pageNo = ref(0)
// [zh] 声明变量 `pageTotal`
const pageTotal = ref(1)
// [zh] 声明变量 `loading`
const loading = ref(false)
// [zh] 声明变量 `finished`
const finished = ref(false)
// [zh] 声明变量 `sentinel`
const sentinel = ref(null)
// [zh] 声明变量 `observer`
let observer = null

// [zh] 声明变量 `loadList`
const loadList = async (reset = false) => {
  // [zh] 条件判断 if
  if (loading.value) return
  // [zh] 条件判断 if
  if (reset) {
    // [zh] TypeScript/JS 语句：`pageNo.value = 0`
    pageNo.value = 0
    // [zh] TypeScript/JS 语句：`pageTotal.value = 1`
    pageTotal.value = 1
    // [zh] TypeScript/JS 语句：`finished.value = false`
    finished.value = false
    // [zh] TypeScript/JS 语句：`list.value = []`
    list.value = []
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (finished.value) return
  // [zh] TypeScript/JS 语句：`loading.value = true`
  loading.value = true
  // [zh] 代码块开始
  try {
    // [zh] 声明变量 `next`
    const next = pageNo.value + 1
    // [zh] 声明变量 `params`
    const params = { pageNo: next, pageSize: 8 }
    // [zh] 条件判断 if
    if (searchForm.nickNameFuzzy) params.nickNameFuzzy = searchForm.nickNameFuzzy
    // [zh] 条件判断 if
    if (searchForm.productNameFuzzy) params.productNameFuzzy = searchForm.productNameFuzzy
    // [zh] 声明变量 `result`
    const result = await proxy.Request({ url: proxy.Api.loadComment, params, showLoading: false })
    // [zh] 条件判断 if
    if (!result) return
    // [zh] 声明变量 `data`
    const data = result.data || {}
    // [zh] 声明变量 `chunk`
    const chunk = (data.list || []).map((item) => ({
      // [zh] TypeScript/JS 语句：`...item,`
      ...item,
      // [zh] TypeScript/JS 语句：`commentImages: item.commentImages ? String(item.comment`
      commentImages: item.commentImages ? String(item.commentImages).split(',').filter(Boolean) : [],
      // [zh] TypeScript/JS 语句：`recommentImages: item.recommentImages ? String(item.rec`
      recommentImages: item.recommentImages ? String(item.recommentImages).split(',').filter(Boolean) : []
    // [zh] TypeScript/JS 语句：`}))`
    }))
    // [zh] TypeScript/JS 语句：`list.value = next === 1 ? chunk : list.value.concat(chu`
    list.value = next === 1 ? chunk : list.value.concat(chunk)
    // [zh] TypeScript/JS 语句：`pageNo.value = Number(data.pageNo) || next`
    pageNo.value = Number(data.pageNo) || next
    // [zh] TypeScript/JS 语句：`pageTotal.value = Number(data.pageTotal) || pageNo.valu`
    pageTotal.value = Number(data.pageTotal) || pageNo.value
    // [zh] TypeScript/JS 语句：`finished.value = pageNo.value >= pageTotal.value`
    finished.value = pageNo.value >= pageTotal.value
  // [zh] 代码块开始
  } finally {
    // [zh] TypeScript/JS 语句：`loading.value = false`
    loading.value = false
    // [zh] 条件判断 if
    if (!finished.value && pageNo.value > 0 && sentinel.value) {
      // [zh] 代码块开始
      setTimeout(() => {
        // [zh] 声明变量 `rect`
        const rect = sentinel.value.getBoundingClientRect()
        // [zh] 声明变量 `threshold`
        const threshold = window.innerHeight + 300
        // [zh] 条件判断 if
        if (rect.bottom <= threshold) {
          // [zh] TypeScript/JS 语句：`loadList()`
          loadList()
        // [zh] 语法符号
        }
      // [zh] TypeScript/JS 语句：`}, 200)`
      }, 200)
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
// [zh] 语法符号
}

// [zh] 声明变量 `reload`
const reload = () => loadList(true)

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
// [zh] 声明变量 `reply`
const reply = (row) => commentRef.value.show(row.orderId)

// [zh] 声明变量 `del`
const del = (row) => {
  // [zh] 条件判断 if
  if (row.status == 1) {
    // [zh] TypeScript/JS 语句：`proxy.Message.warning('该评论已删除')`
    proxy.Message.warning('该评论已删除')
    // [zh] TypeScript/JS 语句：`return`
    return
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  proxy.Confirm({
    // [zh] TypeScript/JS 语句：`message: '确定要删除该评价吗？',`
    message: '确定要删除该评价吗？',
    // [zh] 代码块开始
    okfun: async () => {
      // [zh] 声明变量 `result`
      const result = await proxy.Request({ url: proxy.Api.delComment, params: { orderId: row.orderId } })
      // [zh] 条件判断 if
      if (!result) return
      // [zh] TypeScript/JS 语句：`proxy.Message.success('评论已删除')`
      proxy.Message.success('评论已删除')
      // [zh] TypeScript/JS 语句：`reload()`
      reload()
    // [zh] 语法符号
    }
  // [zh] TypeScript/JS 语句：`})`
  })
// [zh] 语法符号
}

// [zh] 代码块开始
onMounted(() => {
  // [zh] TypeScript/JS 语句：`loadList(true)`
  loadList(true)
  // [zh] TypeScript/JS 语句：`observer = new IntersectionObserver(`
  observer = new IntersectionObserver(
    // [zh] 代码块开始
    (entries) => {
      // [zh] 条件判断 if
      if (entries.some((e) => e.isIntersecting)) loadList()
    // [zh] TypeScript/JS 语句：`},`
    },
    // [zh] TypeScript/JS 语句：`{ rootMargin: '0px 0px 300px 0px' }`
    { rootMargin: '0px 0px 300px 0px' }
  // [zh] 语法符号
  )
  // [zh] 条件判断 if
  if (sentinel.value) observer.observe(sentinel.value)
// [zh] TypeScript/JS 语句：`})`
})

// [zh] 代码块开始
onUnmounted(() => {
  // [zh] TypeScript/JS 语句：`observer && observer.disconnect()`
  observer && observer.disconnect()
  // [zh] TypeScript/JS 语句：`observer = null`
  observer = null
// [zh] TypeScript/JS 语句：`})`
})
</script>

/* [zh] 样式声明 */
<style lang="scss" scoped>
/* [zh] 样式规则 `.m-simple {` */
.m-simple {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 12px;` */
  gap: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-search {` */
.m-search {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `gap: 8px;` */
  gap: 8px;
  /* [zh] 样式规则 `height: 44px;` */
  height: 44px;
  /* [zh] 样式规则 `padding: 0 12px;` */
  padding: 0 12px;
  /* [zh] 样式规则 `border-radius: 16px;` */
  border-radius: 16px;

  /* [zh] 样式规则 `.search-input {` */
  .search-input {
    /* [zh] 样式规则 `flex: 1;` */
    flex: 1;
    /* [zh] 样式规则 `min-width: 0;` */
    min-width: 0;
    /* [zh] 样式规则 `border: none;` */
    border: none;
    /* [zh] 样式规则 `background: transparent;` */
    background: transparent;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);
    /* [zh] 样式规则 `outline: none;` */
    outline: none;

    /* [zh] 样式规则 `& + .search-input {` */
    & + .search-input {
      /* [zh] 样式规则 `border-left: 1px solid rgba(120, 120, 12` */
      border-left: 1px solid rgba(120, 120, 128, 0.2);
      /* [zh] 样式规则 `padding-left: 8px;` */
      padding-left: 8px;
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `&::placeholder {` */
    &::placeholder {
      /* [zh] 样式规则 `color: var(--m-ink-3);` */
      color: var(--m-ink-3);
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-list {` */
.m-list {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `flex-direction: column;` */
  flex-direction: column;
  /* [zh] 样式规则 `gap: 10px;` */
  gap: 10px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.cmt-card {` */
.cmt-card {
  /* [zh] 样式规则 `padding: 12px 14px;` */
  padding: 12px 14px;

  /* [zh] 样式规则 `&.cmt-deleted {` */
  &.cmt-deleted {
    /* [zh] 样式规则 `opacity: 0.6;` */
    opacity: 0.6;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cmt-head {` */
  .cmt-head {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `align-items: center;` */
    align-items: center;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;

    /* [zh] 样式规则 `.cmt-nick {` */
    .cmt-nick {
      /* [zh] 样式规则 `font-size: 13px;` */
      font-size: 13px;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
      /* [zh] 样式规则 `color: var(--m-ink);` */
      color: var(--m-ink);
    /* [zh] 样式规则 `}` */
    }

    /* [zh] 样式规则 `.deleted-badge {` */
    .deleted-badge {
      /* [zh] 样式规则 `display: inline-block;` */
      display: inline-block;
      /* [zh] 样式规则 `padding: 2px 8px;` */
      padding: 2px 8px;
      /* [zh] 样式规则 `border-radius: 6px;` */
      border-radius: 6px;
      /* [zh] 样式规则 `background: var(--m-danger);` */
      background: var(--m-danger);
      /* [zh] 样式规则 `color: #fff;` */
      color: #fff;
      /* [zh] 样式规则 `font-size: 10px;` */
      font-size: 10px;
      /* [zh] 样式规则 `font-weight: 600;` */
      font-weight: 600;
      /* [zh] 样式规则 `margin-left: auto;` */
      margin-left: auto;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cmt-product {` */
  .cmt-product {
    /* [zh] 样式规则 `margin: 8px 0 4px;` */
    margin: 8px 0 4px;
    /* [zh] 样式规则 `font-size: 12px;` */
    font-size: 12px;
    /* [zh] 样式规则 `color: var(--m-ink-3);` */
    color: var(--m-ink-3);
    /* [zh] 样式规则 `.more-products-btn {` */
    .more-products-btn {
      /* [zh] 样式规则 `display: inline-block;` */
      display: inline-block;
      /* [zh] 样式规则 `margin-left: 6px;` */
      margin-left: 6px;
      /* [zh] 样式规则 `padding: 1px 6px;` */
      padding: 1px 6px;
      /* [zh] 样式规则 `border-radius: 4px;` */
      border-radius: 4px;
      /* [zh] 样式规则 `background: var(--m-gold-soft);` */
      background: var(--m-gold-soft);
      /* [zh] 样式规则 `color: #927535;` */
      color: #927535;
      /* [zh] 样式规则 `font-size: 10px;` */
      font-size: 10px;
      /* [zh] 样式规则 `cursor: pointer;` */
      cursor: pointer;
      /* [zh] 样式规则 `white-space: nowrap;` */
      white-space: nowrap;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cmt-content {` */
  .cmt-content {
    /* [zh] 样式规则 `margin: 0 0 6px;` */
    margin: 0 0 6px;
    /* [zh] 样式规则 `font-size: 14px;` */
    font-size: 14px;
    /* [zh] 样式规则 `color: var(--m-ink);` */
    color: var(--m-ink);
    /* [zh] 样式规则 `line-height: 1.5;` */
    line-height: 1.5;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cmt-imgs {` */
  .cmt-imgs {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `flex-wrap: wrap;` */
    flex-wrap: wrap;
    /* [zh] 样式规则 `gap: 6px;` */
    gap: 6px;
    /* [zh] 样式规则 `margin-bottom: 6px;` */
    margin-bottom: 6px;
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式声明 */
  .cmt-recomment,
  /* [zh] 样式规则 `.cmt-reply {` */
  .cmt-reply {
    /* [zh] 样式规则 `margin: 6px 0 0;` */
    margin: 6px 0 0;
    /* [zh] 样式规则 `font-size: 13px;` */
    font-size: 13px;
    /* [zh] 样式规则 `line-height: 1.45;` */
    line-height: 1.45;
    /* [zh] 样式规则 `color: var(--m-ink-2);` */
    color: var(--m-ink-2);

    /* [zh] 样式规则 `.tag {` */
    .tag {
      /* [zh] 样式规则 `display: inline-block;` */
      display: inline-block;
      /* [zh] 样式规则 `margin-right: 6px;` */
      margin-right: 6px;
      /* [zh] 样式规则 `padding: 0 6px;` */
      padding: 0 6px;
      /* [zh] 样式规则 `border-radius: 6px;` */
      border-radius: 6px;
      /* [zh] 样式规则 `background: var(--m-gold-soft);` */
      background: var(--m-gold-soft);
      /* [zh] 样式规则 `color: #927535;` */
      color: #927535;
      /* [zh] 样式规则 `font-size: 11px;` */
      font-size: 11px;
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cmt-reply {` */
  .cmt-reply {
    /* [zh] 样式规则 `padding: 8px 10px;` */
    padding: 8px 10px;
    /* [zh] 样式规则 `margin-top: 8px;` */
    margin-top: 8px;
    /* [zh] 样式规则 `border-radius: 10px;` */
    border-radius: 10px;
    /* [zh] 样式规则 `background: rgba(120, 120, 128, 0.08);` */
    background: rgba(120, 120, 128, 0.08);
  /* [zh] 样式规则 `}` */
  }

  /* [zh] 样式规则 `.cmt-ops {` */
  .cmt-ops {
    /* [zh] 样式规则 `display: flex;` */
    display: flex;
    /* [zh] 样式规则 `gap: 8px;` */
    gap: 8px;
    /* [zh] 样式规则 `margin-top: 10px;` */
    margin-top: 10px;

    /* [zh] 样式规则 `.op-btn {` */
    .op-btn {
      /* [zh] 样式规则 `flex: 1;` */
      flex: 1;
      /* [zh] 样式规则 `height: 32px;` */
      height: 32px;
      /* [zh] 样式规则 `border: 1px solid rgba(120, 120, 128, 0.` */
      border: 1px solid rgba(120, 120, 128, 0.24);
      /* [zh] 样式规则 `border-radius: 10px;` */
      border-radius: 10px;
      /* [zh] 样式规则 `background: rgba(255, 255, 255, 0.5);` */
      background: rgba(255, 255, 255, 0.5);
      /* [zh] 样式规则 `color: var(--m-ink-2);` */
      color: var(--m-ink-2);
      /* [zh] 样式规则 `font-size: 12px;` */
      font-size: 12px;
      /* [zh] 样式规则 `cursor: pointer;` */
      cursor: pointer;

      /* [zh] 样式规则 `&.danger {` */
      &.danger {
        /* [zh] 样式规则 `color: var(--m-danger);` */
        color: var(--m-danger);
        /* [zh] 样式规则 `border-color: rgba(255, 59, 48, 0.3);` */
        border-color: rgba(255, 59, 48, 0.3);
      /* [zh] 样式规则 `}` */
      }
    /* [zh] 样式规则 `}` */
    }
  /* [zh] 样式规则 `}` */
  }
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-sentinel {` */
.m-sentinel {
  /* [zh] 样式规则 `display: flex;` */
  display: flex;
  /* [zh] 样式规则 `align-items: center;` */
  align-items: center;
  /* [zh] 样式规则 `justify-content: center;` */
  justify-content: center;
  /* [zh] 样式规则 `min-height: 28px;` */
  min-height: 28px;
  /* [zh] 样式规则 `font-size: 12px;` */
  font-size: 12px;
  /* [zh] 样式规则 `color: var(--m-ink-3);` */
  color: var(--m-ink-3);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.m-empty-tip {` */
.m-empty-tip {
  /* [zh] 样式规则 `margin: 24px 0;` */
  margin: 24px 0;
  /* [zh] 样式规则 `text-align: center;` */
  text-align: center;
  /* [zh] 样式规则 `font-size: 14px;` */
  font-size: 14px;
  /* [zh] 样式规则 `color: var(--m-ink-3);` */
  color: var(--m-ink-3);
/* [zh] 样式规则 `}` */
}
</style>
