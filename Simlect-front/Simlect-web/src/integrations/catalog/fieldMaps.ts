

// [zh] 导出模块成员
export interface SimlectCategoryNode {
  // [zh] 语句
  id?: string;
  // [zh] 语句
  cate_name?: string;
  // [zh] 语句
  pic?: string;
  // [zh] 语句
  children?: SimlectCategoryNode[];
// [zh] 语法符号
}

// [zh] 导出模块成员
export interface EshopCategoryNode {
  // [zh] 语句
  categoryId: string;
  // [zh] 语句
  categoryName: string;
  // [zh] 语句
  children?: EshopCategoryNode[];
// [zh] 语法符号
}


// [zh] 导出模块成员
export function categoryToSimlectShape(nodes: EshopCategoryNode[]): SimlectCategoryNode[] {
  // [zh] return 返回
  return nodes.map((n) => ({
    // [zh] TypeScript/JS 语句：`id: n.categoryId,`
    id: n.categoryId,
    // [zh] TypeScript/JS 语句：`cate_name: n.categoryName,`
    cate_name: n.categoryName,
    // [zh] TypeScript/JS 语句：`pic: '',`
    pic: '',
    // [zh] TypeScript/JS 语句：`children: n.children?.length ? categoryToSimlectShape(n`
    children: n.children?.length ? categoryToSimlectShape(n.children) : []
  // [zh] 语句
  }));
// [zh] 语法符号
}


// [zh] 导出模块成员
export function productDisplayName(p: Record<string, unknown>): string {
  // [zh] return 返回
  return String(p.productName ?? p.name ?? '');
// [zh] 语法符号
}

// [zh] 导出模块成员
export function productPriceText(p: Record<string, unknown>): string {
  // [zh] 声明变量 `val`
  const val = p.minPrice ?? p.price ?? p.salePrice;
  // [zh] return 返回
  return val != null ? Number(val).toFixed(2) : '--';
// [zh] 语法符号
}
