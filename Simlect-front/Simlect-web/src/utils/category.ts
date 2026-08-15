
// [zh] 导出模块成员
export function normalizeCategoryTree(data: unknown): any[] {
  // [zh] 条件判断 if
  if (!Array.isArray(data) || !data.length) return [];

  // [zh] 声明变量 `hasNestedChildren`
  const hasNestedChildren = data.some((item) => Array.isArray(item?.children));
  // [zh] 条件判断 if
  if (hasNestedChildren) {
    // [zh] return 返回
    return data.filter((item) => !item.pCategoryId || item.pCategoryId === '0');
  // [zh] 语法符号
  }

  // [zh] 声明变量 `list`
  const list = data as any[];
  // [zh] 声明变量 `roots`
  const roots = list.filter((c) => !c.pCategoryId || c.pCategoryId === '0');
  // [zh] return 返回
  return roots.map((root) => ({
    // [zh] TypeScript/JS 语句：`...root,`
    ...root,
    // [zh] TypeScript/JS 语句：`children: list.filter((c) => c.pCategoryId === root.cat`
    children: list.filter((c) => c.pCategoryId === root.categoryId)
  // [zh] 语句
  }));
// [zh] 语法符号
}

// [zh] 导出模块成员
export function countCategoryNodes(roots: any[]): number {
  // [zh] 声明变量 `n`
  let n = 0;
  // [zh] 声明变量 `walk`
  const walk = (items: any[]) => {
    // [zh] 代码块开始
    items.forEach((item) => {
      // [zh] 语句
      n += 1;
      // [zh] 条件判断 if
      if (item.children?.length) walk(item.children);
    // [zh] 语句
    });
  // [zh] 语句
  };
  // [zh] 语句
  walk(roots);
  // [zh] return 返回
  return n;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function findCategoryInTree(roots: any[], categoryId: string): any | null {
  // [zh] for 循环
  for (const root of roots) {
    // [zh] 条件判断 if
    if (String(root.categoryId) === String(categoryId)) return root;
    // [zh] for 循环
    for (const child of root.children || []) {
      // [zh] 条件判断 if
      if (String(child.categoryId) === String(categoryId)) return child;
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] return 返回
  return null;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function findParentCategory(roots: any[], categoryId: string): any | null {
  // [zh] for 循环
  for (const root of roots) {
    // [zh] 条件判断 if
    if (root.children?.some((c: any) => String(c.categoryId) === String(categoryId))) {
      // [zh] return 返回
      return root;
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] return 返回
  return null;
// [zh] 语法符号
}

// [zh] 导出模块成员
export type FlattenCategoryLabelMode = 'child' | 'full';

// [zh] 导出模块成员
export function flattenCategoryOptions(
  // [zh] TypeScript/JS 语句：`roots: any[],`
  roots: any[],
  // [zh] TypeScript/JS 语句：`labelMode: FlattenCategoryLabelMode = 'full'`
  labelMode: FlattenCategoryLabelMode = 'full'
// [zh] 代码块开始
): { categoryId: string; categoryName: string }[] {
  // [zh] 声明变量 `out`
  const out: { categoryId: string; categoryName: string }[] = [];
  // [zh] 代码块开始
  roots.forEach((root) => {
    // [zh] 条件判断 if
    if (root.children?.length) {
      // [zh] 代码块开始
      root.children.forEach((sub: any) => {
        // [zh] 代码块开始
        out.push({
          // [zh] TypeScript/JS 语句：`categoryId: sub.categoryId,`
          categoryId: sub.categoryId,
          // [zh] TypeScript/JS 语句：`categoryName:`
          categoryName:
            // [zh] TypeScript/JS 语句：`labelMode === 'child' ? sub.categoryName : `${root.cate`
            labelMode === 'child' ? sub.categoryName : `${root.categoryName} / ${sub.categoryName}`
        // [zh] 语句
        });
      // [zh] 语句
      });
    // [zh] 代码块开始
    } else {
      // [zh] 语句
      out.push({ categoryId: root.categoryId, categoryName: root.categoryName });
    // [zh] 语法符号
    }
  // [zh] 语句
  });
  // [zh] return 返回
  return out;
// [zh] 语法符号
}
