
export function normalizeCategoryTree(data: unknown): any[] {
  if (!Array.isArray(data) || !data.length) return [];

  const hasNestedChildren = data.some((item) => Array.isArray(item?.children));
  if (hasNestedChildren) {
    return data.filter((item) => !item.pCategoryId || item.pCategoryId === '0');
  }

  const list = data as any[];
  const roots = list.filter((c) => !c.pCategoryId || c.pCategoryId === '0');
  return roots.map((root) => ({
    ...root,
    children: list.filter((c) => c.pCategoryId === root.categoryId)
  }));
}

export function countCategoryNodes(roots: any[]): number {
  let n = 0;
  const walk = (items: any[]) => {
    items.forEach((item) => {
      n += 1;
      if (item.children?.length) walk(item.children);
    });
  };
  walk(roots);
  return n;
}

export function findCategoryInTree(roots: any[], categoryId: string): any | null {
  for (const root of roots) {
    if (String(root.categoryId) === String(categoryId)) return root;
    for (const child of root.children || []) {
      if (String(child.categoryId) === String(categoryId)) return child;
    }
  }
  return null;
}

export function findParentCategory(roots: any[], categoryId: string): any | null {
  for (const root of roots) {
    if (root.children?.some((c: any) => String(c.categoryId) === String(categoryId))) {
      return root;
    }
  }
  return null;
}

export type FlattenCategoryLabelMode = 'child' | 'full';

export function flattenCategoryOptions(
  roots: any[],
  labelMode: FlattenCategoryLabelMode = 'full'
): { categoryId: string; categoryName: string }[] {
  const out: { categoryId: string; categoryName: string }[] = [];
  roots.forEach((root) => {
    if (root.children?.length) {
      root.children.forEach((sub: any) => {
        out.push({
          categoryId: sub.categoryId,
          categoryName:
            labelMode === 'child' ? sub.categoryName : `${root.categoryName} / ${sub.categoryName}`
        });
      });
    } else {
      out.push({ categoryId: root.categoryId, categoryName: root.categoryName });
    }
  });
  return out;
}
