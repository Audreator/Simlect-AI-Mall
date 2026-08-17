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
<script setup>
import { ref } from "vue";

const emit = defineEmits(["rowSelected", "rowClick"]);
const props = defineProps({
  dataSource: Object,
  showPagination: {
    type: Boolean,
    default: true,
  },
  options: {
    type: Object,
    default: {},
  },
  columns: Array,
  fetch: Function,
  initFetch: {
    type: Boolean,
    default: true,
  },
  selected: Function,
});

const init = () => {
  if (props.initFetch && props.fetch) {
    props.fetch();
  }
};
init();

const dataTable = ref();

const clearSelection = () => {
  dataTable.value.clearSelection();
};

const setCurrentRow = (rowKey, rowValue) => {
  let row = props.dataSource.list.find((item) => {
    return item[rowKey] === rowValue;
  });
  dataTable.value.setCurrentRow(row);
};

defineExpose({ setCurrentRow, clearSelection });

const handleRowClick = (row) => {
  emit("rowClick", row);
};

const handleSelectionChange = (row) => {
  emit("rowSelected", row);
};

const handlePageSizeChange = (size) => {
  props.dataSource.pageSize = size;
  props.dataSource.pageNo = 1;
  props.fetch();
};

const handlePageNoChange = (pageNo) => {
  props.dataSource.pageNo = pageNo;
  props.fetch();
};

const selectedHandler = (row, index) => {
  if (props.selected) {
    return props.selected(row, index);
  }
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
