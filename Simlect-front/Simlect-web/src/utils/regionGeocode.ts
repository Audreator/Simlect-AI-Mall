// [zh] 导入 `element-china-area-data`
import { regionData } from 'element-china-area-data';

// [zh] TypeScript 类型 `RegionNode`（类比 Java interface/DTO）
type RegionNode = { label: string; value: string; children?: RegionNode[] };

// [zh] 声明变量 `DIRECT_MUNICIPALITY_KEYS`
const DIRECT_MUNICIPALITY_KEYS = ['北京', '上海', '天津', '重庆'];

// [zh] 声明变量 `normalizeKey`
const normalizeKey = (s?: string | null): string => {
  // [zh] 声明变量 `raw`
  const raw = (s || '').trim().replace(/\s+/g, '');
  // [zh] 条件判断 if
  if (!raw || raw === '市辖区') return raw;
  // [zh] return 返回
  return raw.replace(
    // [zh] TypeScript/JS 语句：`/(省|市|自治区|特别行政区|壮族|回族|维吾尔|地区|盟|自治州|自治县|林区|特区|县|区|旗)$/g,`
    /(省|市|自治区|特别行政区|壮族|回族|维吾尔|地区|盟|自治州|自治县|林区|特区|县|区|旗)$/g,
    // [zh] TypeScript/JS 语句：`''`
    ''
  // [zh] 语句
  );
// [zh] 语句
};

// [zh] 声明变量 `labelMatch`
const labelMatch = (label: string, target?: string | null): boolean => {
  // [zh] 条件判断 if
  if (!target) return false;
  // [zh] 声明变量 `a`
  const a = normalizeKey(label);
  // [zh] 声明变量 `b`
  const b = normalizeKey(target);
  // [zh] 条件判断 if
  if (!a || !b) return false;
  // [zh] return 返回
  return a === b || a.includes(b) || b.includes(a);
// [zh] 语句
};

// [zh] 声明变量 `isDirectMunicipality`
const isDirectMunicipality = (...names: (string | null | undefined)[]): boolean =>
  // [zh] 代码块开始
  names.some((n) => {
    // [zh] 声明变量 `k`
    const k = normalizeKey(n);
    // [zh] return 返回
    return DIRECT_MUNICIPALITY_KEYS.includes(k);
  // [zh] 语句
  });

// [zh] 导出模块成员
export const matchRegionFromFullAddress = (address: string): string[] | null => {
  // [zh] 声明变量 `text`
  const text = (address || '').trim();
  // [zh] 条件判断 if
  if (!text) return null;

  // [zh] 声明变量 `best`
  let best: { codes: string[]; len: number } | null = null;

  // [zh] 声明变量 `consider`
  const consider = (combo: string, codes: string[]) => {
    // [zh] 条件判断 if
    if (!combo || !text.includes(combo)) return;
    // [zh] 条件判断 if
    if (!best || combo.length > best.len) {
      // [zh] 语句
      best = { codes, len: combo.length };
    // [zh] 语法符号
    }
  // [zh] 语句
  };

  // [zh] for 循环
  for (const p of regionData as RegionNode[]) {
    // [zh] 声明变量 `pl`
    const pl = String(p.label || '');
    // [zh] for 循环
    for (const c of p.children || []) {
      // [zh] 声明变量 `cl`
      const cl = String(c.label || '');
      // [zh] 声明变量 `subs`
      const subs = c.children || [];
      // [zh] for 循环
      for (const d of subs) {
        // [zh] 声明变量 `dl`
        const dl = String(d.label || '');
        // [zh] 声明变量 `codes`
        const codes = [String(p.value), String(c.value), String(d.value)];
        // [zh] 语句
        consider(pl + cl + dl, codes);
        // [zh] 语句
        consider(cl + dl, codes);
        // [zh] 语句
        consider(dl, codes);
      // [zh] 语法符号
      }
      // [zh] 语句
      consider(pl + cl, [String(p.value), String(c.value)]);
      // [zh] 条件判断 if
      if (!subs.length) consider(cl, [String(p.value), String(c.value)]);
    // [zh] 语法符号
    }
    // [zh] 语句
    consider(pl, [String(p.value)]);
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (!best) return null;
  // [zh] return 返回
  return (best as { codes: string[]; len: number }).codes;
// [zh] 语句
};

// [zh] 声明变量 `resolveMunicipalityCodes`
const resolveMunicipalityCodes = (
  // [zh] TypeScript/JS 语句：`province?: string | null,`
  province?: string | null,
  // [zh] TypeScript/JS 语句：`district?: string | null`
  district?: string | null
// [zh] 代码块开始
): string[] | null => {
  // [zh] for 循环
  for (const p of regionData as RegionNode[]) {
    // [zh] 条件判断 if
    if (!isDirectMunicipality(p.label, province)) continue;
    // [zh] 声明变量 `cityLevel`
    const cityLevel =
      // [zh] 语句
      (p.children || []).find((c) => labelMatch(c.label, '市辖区')) || p.children?.[0];
    // [zh] 条件判断 if
    if (!cityLevel) return [String(p.value)];
    // [zh] 条件判断 if
    if (!district) return [String(p.value), String(cityLevel.value)];
    // [zh] for 循环
    for (const d of cityLevel.children || []) {
      // [zh] 条件判断 if
      if (labelMatch(d.label, district)) {
        // [zh] return 返回
        return [String(p.value), String(cityLevel.value), String(d.value)];
      // [zh] 语法符号
      }
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] return 返回
  return null;
// [zh] 语句
};

// [zh] 声明变量 `findBestByDistrict`
const findBestByDistrict = (
  // [zh] TypeScript/JS 语句：`province?: string | null,`
  province?: string | null,
  // [zh] TypeScript/JS 语句：`city?: string | null,`
  city?: string | null,
  // [zh] TypeScript/JS 语句：`district?: string | null`
  district?: string | null
// [zh] 代码块开始
): string[] | null => {
  // [zh] 条件判断 if
  if (!district) return null;
  // [zh] 声明变量 `best`
  let best: { codes: string[]; score: number } | null = null;

  // [zh] for 循环
  for (const p of regionData as RegionNode[]) {
    // [zh] for 循环
    for (const c of p.children || []) {
      // [zh] for 循环
      for (const d of c.children || []) {
        // [zh] 声明变量 `score`
        let score = 0;
        // [zh] 条件判断 if
        if (labelMatch(d.label, district)) score += 40;
        // [zh] 条件判断 if
        if (city && labelMatch(c.label, city)) score += 20;
        // [zh] 条件判断 if
        if (province && labelMatch(p.label, province)) score += 20;
        // [zh] 条件判断 if
        if (score < 40) continue;
        // [zh] 条件判断 if
        if (!best || score > best.score) {
          // [zh] 语句
          best = { codes: [String(p.value), String(c.value), String(d.value)], score };
        // [zh] 语法符号
        }
      // [zh] 语法符号
      }
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] 条件判断 if
  if (!best) return null;
  // [zh] return 返回
  return best.codes;
// [zh] 语句
};

// [zh] 声明变量 `findByProvinceAndCity`
const findByProvinceAndCity = (
  // [zh] TypeScript/JS 语句：`province?: string | null,`
  province?: string | null,
  // [zh] TypeScript/JS 语句：`city?: string | null`
  city?: string | null
// [zh] 代码块开始
): string[] | null => {
  // [zh] 条件判断 if
  if (!city) return null;
  // [zh] 声明变量 `fallback`
  let fallback: string[] | null = null;
  // [zh] for 循环
  for (const p of regionData as RegionNode[]) {
    // [zh] for 循环
    for (const c of p.children || []) {
      // [zh] 条件判断 if
      if (!labelMatch(c.label, city)) continue;
      // [zh] 声明变量 `sub`
      const sub = c.children || [];
      // [zh] 声明变量 `codes`
      const codes =
        // [zh] TypeScript/JS 语句：`sub.length === 1`
        sub.length === 1
          // [zh] TypeScript/JS 语句：`? [String(p.value), String(c.value), String(sub[0].valu`
          ? [String(p.value), String(c.value), String(sub[0].value)]
          // [zh] 语句
          : [String(p.value), String(c.value)];
      // [zh] 条件判断 if
      if (province && labelMatch(p.label, province)) return codes;
      // [zh] 条件判断 if
      if (!fallback) fallback = codes;
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }
  // [zh] return 返回
  return fallback;
// [zh] 语句
};

// [zh] 导出模块成员
export const resolveRegionCodes = (
  // [zh] TypeScript/JS 语句：`province?: string | null,`
  province?: string | null,
  // [zh] TypeScript/JS 语句：`city?: string | null,`
  city?: string | null,
  // [zh] TypeScript/JS 语句：`district?: string | null`
  district?: string | null
// [zh] 代码块开始
): string[] | null => {
  // [zh] 条件判断 if
  if (!province && !city && !district) return null;

  // [zh] 声明变量 `fullText`
  const fullText = [province, city, district].filter(Boolean).join('');
  // [zh] 条件判断 if
  if (fullText) {
    // [zh] 声明变量 `byText`
    const byText = matchRegionFromFullAddress(fullText);
    // [zh] 条件判断 if
    if (byText?.length) return byText;
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (isDirectMunicipality(province, city)) {
    // [zh] 声明变量 `muni`
    const muni = resolveMunicipalityCodes(province || city, district);
    // [zh] 条件判断 if
    if (muni?.length) return muni;
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (district) {
    // [zh] 声明变量 `byDistrict`
    const byDistrict = findBestByDistrict(province, city, district);
    // [zh] 条件判断 if
    if (byDistrict?.length) return byDistrict;
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (city && !district) {
    // [zh] 声明变量 `byCity`
    const byCity = findByProvinceAndCity(province, city);
    // [zh] 条件判断 if
    if (byCity?.length) return byCity;
  // [zh] 语法符号
  }

  // [zh] for 循环
  for (const p of regionData as RegionNode[]) {
    // [zh] 声明变量 `provMatch`
    const provMatch =
      // [zh] TypeScript/JS 语句：`!province ||`
      !province ||
      // [zh] TypeScript/JS 语句：`labelMatch(p.label, province) ||`
      labelMatch(p.label, province) ||
      // [zh] TypeScript/JS 语句：`labelMatch(p.label, city) ||`
      labelMatch(p.label, city) ||
      // [zh] 语句
      (isDirectMunicipality(province) && isDirectMunicipality(p.label));
    // [zh] 条件判断 if
    if (!provMatch) continue;

    // [zh] 声明变量 `children`
    const children = p.children || [];
    // [zh] 条件判断 if
    if (!children.length) return [String(p.value)];

    // [zh] for 循环
    for (const c of children) {
      // [zh] 声明变量 `cityMatch`
      const cityMatch =
        // [zh] TypeScript/JS 语句：`!city ||`
        !city ||
        // [zh] TypeScript/JS 语句：`labelMatch(c.label, city) ||`
        labelMatch(c.label, city) ||
        // [zh] 语句
        (isDirectMunicipality(province, city) && labelMatch(c.label, '市辖区'));

      // [zh] 条件判断 if
      if (!cityMatch) continue;

      // [zh] 声明变量 `sub`
      const sub = c.children || [];
      // [zh] 条件判断 if
      if (district && sub.length) {
        // [zh] for 循环
        for (const d of sub) {
          // [zh] 条件判断 if
          if (labelMatch(d.label, district)) {
            // [zh] return 返回
            return [String(p.value), String(c.value), String(d.value)];
          // [zh] 语法符号
          }
        // [zh] 语法符号
        }
      // [zh] 语法符号
      }
      // [zh] 条件判断 if
      if (sub.length === 1) {
        // [zh] return 返回
        return [String(p.value), String(c.value), String(sub[0].value)];
      // [zh] 语法符号
      }
      // [zh] 条件判断 if
      if (sub.length) return [String(p.value), String(c.value)];
      // [zh] return 返回
      return [String(p.value), String(c.value)];
    // [zh] 语法符号
    }

    // [zh] 条件判断 if
    if (district) {
      // [zh] for 循环
      for (const c of children) {
        // [zh] for 循环
        for (const d of c.children || []) {
          // [zh] 条件判断 if
          if (labelMatch(d.label, district)) {
            // [zh] return 返回
            return [String(p.value), String(c.value), String(d.value)];
          // [zh] 语法符号
          }
        // [zh] 语法符号
        }
      // [zh] 语法符号
      }
    // [zh] 语法符号
    }
  // [zh] 语法符号
  }

  // [zh] return 返回
  return null;
// [zh] 语句
};
