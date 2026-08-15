<!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
<template>
  <!-- [zh] 开始标签 `<div>` -->
  <div class="table-height">
    <!-- [zh] 开始标签 `<el-table>` -->
    <el-table ref="dataTable" :data="dataSource.list || []" height="100%" :stripe="options.stripe"
      :border="options.border" header-row-class-name="table-header-row" highlight-current-row
      @row-click="handleRowClick" @selection-change="handleSelectionChange">

      <!-- [zh] 开始标签 `<el-table-column>` -->
      <el-table-column v-if="options.selectType && options.selectType == 'checkbox'" type="selection"
        :selectable="selectedHandler" width="50" align="center"></el-table-column>

      <!-- [zh] 开始标签 `<el-table-column>` -->
      <el-table-column v-if="options.showIndex" label="序号" type="index" width="60" align="center"></el-table-column>

      <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
      <template v-for="(column, index) in columns">
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template v-if="column.scopedSlots">
          <!-- [zh] 开始标签 `<el-table-column>` -->
          <el-table-column :key="index" :prop="column.prop" :label="column.label" :align="column.align || 'left'"
            :width="column.width">
            <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
            <template #default="scope">
              <!-- [zh] 开始标签 `<slot>` -->
              <slot :name="column.scopedSlots" :index="scope.$index" :row="scope.row">
              <!-- [zh] 闭合标签 `</slot>` -->
              </slot>
            </template>
          </el-table-column>
        </template>
        <!-- [zh] SFC 区块开始（单文件组件 template/script/style 三段） -->
        <template v-else>
          <!-- [zh] 开始标签 `<el-table-column>` -->
          <el-table-column :key="index" :prop="column.prop" :label="column.label" :align="column.align || 'left'"
            :width="column.width" :fixed="column.fixed">
          <!-- [zh] 闭合标签 `</el-table-column>` -->
          </el-table-column>
        </template>
      </template>
    </el-table>
  </div>

  <div class="pagination" v-if="showPagination">
    <el-pagination v-if="dataSource.totalCount" background :total="dataSource.totalCount"
      :page-sizes="[15, 30, 50, 100]" :page-size="dataSource.pageSize" :current-page.sync="dataSource.pageNo"
      layout="total, sizes, prev, pager, next, jumper" @size-change="handlePageSizeChange"
      @current-change="handlePageNoChange" style="text-align: right"></el-pagination>
  </div>
</template>
// [zh] TypeScript/JS 语句：`<script setup>`
<script setup>
// [zh] 导入 `vue`
import { ref } from "vue";

// [zh] 声明变量 `emit`
const emit = defineEmits(["rowSelected", "rowClick"]);
// [zh] 声明变量 `props`
const props = defineProps({
  // [zh] TypeScript/JS 语句：`dataSource: Object,`
  dataSource: Object,
  // [zh] 代码块开始
  showPagination: {
    // [zh] TypeScript/JS 语句：`type: Boolean,`
    type: Boolean,
    // [zh] TypeScript/JS 语句：`default: true,`
    default: true,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] 代码块开始
  options: {
    // [zh] TypeScript/JS 语句：`type: Object,`
    type: Object,
    // [zh] TypeScript/JS 语句：`default: {},`
    default: {},
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] TypeScript/JS 语句：`columns: Array,`
  columns: Array,
  // [zh] TypeScript/JS 语句：`fetch: Function,`
  fetch: Function,
  // [zh] 代码块开始
  initFetch: {
    // [zh] TypeScript/JS 语句：`type: Boolean,`
    type: Boolean,
    // [zh] TypeScript/JS 语句：`default: true,`
    default: true,
  // [zh] TypeScript/JS 语句：`},`
  },
  // [zh] TypeScript/JS 语句：`selected: Function,`
  selected: Function,
// [zh] 语句
});

// [zh] 声明变量 `init`
const init = () => {
  // [zh] 条件判断 if
  if (props.initFetch && props.fetch) {
    // [zh] 语句
    props.fetch();
  // [zh] 语法符号
  }
// [zh] 语句
};
// [zh] 语句
init();

// [zh] 声明变量 `dataTable`
const dataTable = ref();

// [zh] 声明变量 `clearSelection`
const clearSelection = () => {
  // [zh] 语句
  dataTable.value.clearSelection();
// [zh] 语句
};

// [zh] 声明变量 `setCurrentRow`
const setCurrentRow = (rowKey, rowValue) => {
  // [zh] 声明变量 `row`
  let row = props.dataSource.list.find((item) => {
    // [zh] return 返回
    return item[rowKey] === rowValue;
  // [zh] 语句
  });
  // [zh] 语句
  dataTable.value.setCurrentRow(row);
// [zh] 语句
};

// [zh] 语句
defineExpose({ setCurrentRow, clearSelection });

// [zh] 声明变量 `handleRowClick`
const handleRowClick = (row) => {
  // [zh] 语句
  emit("rowClick", row);
// [zh] 语句
};

// [zh] 声明变量 `handleSelectionChange`
const handleSelectionChange = (row) => {
  // [zh] 语句
  emit("rowSelected", row);
// [zh] 语句
};

// [zh] 声明变量 `handlePageSizeChange`
const handlePageSizeChange = (size) => {
  // [zh] 语句
  props.dataSource.pageSize = size;
  // [zh] 语句
  props.dataSource.pageNo = 1;
  // [zh] 语句
  props.fetch();
// [zh] 语句
};

// [zh] 声明变量 `handlePageNoChange`
const handlePageNoChange = (pageNo) => {
  // [zh] 语句
  props.dataSource.pageNo = pageNo;
  // [zh] 语句
  props.fetch();
// [zh] 语句
};

// [zh] 声明变量 `selectedHandler`
const selectedHandler = (row, index) => {
  // [zh] 条件判断 if
  if (props.selected) {
    // [zh] return 返回
    return props.selected(row, index);
  // [zh] 语法符号
  }
// [zh] 语句
};
</script>
/* [zh] 样式声明 */
<style lang="scss">
/* [zh] 样式规则 `.table-height {` */
.table-height {
  /* [zh] 样式规则 `height: calc(100% - 40px);` */
  height: calc(100% - 40px);
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.pagination {` */
.pagination {
  /* [zh] 样式规则 `padding-top: 12px;` */
  padding-top: 12px;
/* [zh] 样式规则 `}` */
}

/* [zh] 样式规则 `.el-pagination {` */
.el-pagination {
  /* [zh] 样式规则 `justify-content: right;` */
  justify-content: right;
/* [zh] 样式规则 `}` */
}
</style>
