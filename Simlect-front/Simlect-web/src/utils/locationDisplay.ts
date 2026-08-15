// [zh] 导入 `模块`
import type { LocationPayload } from '@/api/location';

// [zh] 导出模块成员
export const simplifyCityName = (name?: string | null) => {
  // [zh] 条件判断 if
  if (!name) return '';
  // [zh] return 返回
  return String(name)
    // [zh] TypeScript/JS 语句：`.trim()`
    .trim()
    // [zh] TypeScript/JS 语句：`.replace(/(特别行政区|自治区|自治州|地区|盟|省|市|区|县)$/g, '')`
    .replace(/(特别行政区|自治区|自治州|地区|盟|省|市|区|县)$/g, '')
    // [zh] 语句
    .trim();
// [zh] 语句
};

// [zh] 导出模块成员
export const parseCityFromSummary = (summary?: string | null) => {
  // [zh] 条件判断 if
  if (!summary) return '';
  // [zh] 声明变量 `first`
  const first = summary.trim().split(/\s+/)[0];
  // [zh] 条件判断 if
  if (!first || first === '当地' || first === '—') return '';
  // [zh] return 返回
  return first;
// [zh] 语句
};

// [zh] 导出模块成员
export const resolveCityLabel = (data?: LocationPayload | null, loading = false): string => {
  // [zh] 条件判断 if
  if (!data) return loading ? '…' : '';

  // [zh] 声明变量 `raw`
  const raw =
    // [zh] TypeScript/JS 语句：`data.city ||`
    data.city ||
    // [zh] TypeScript/JS 语句：`data.district ||`
    data.district ||
    // [zh] TypeScript/JS 语句：`data.province ||`
    data.province ||
    // [zh] TypeScript/JS 语句：`parseCityFromSummary(data.summary) ||`
    parseCityFromSummary(data.summary) ||
    // [zh] 语句
    '';

  // [zh] 声明变量 `label`
  const label = simplifyCityName(raw);
  // [zh] 条件判断 if
  if (label) return label;

  // [zh] 声明变量 `fromSummary`
  const fromSummary = simplifyCityName(parseCityFromSummary(data.summary));
  // [zh] 条件判断 if
  if (fromSummary) return fromSummary;

  // [zh] return 返回
  return loading ? '…' : '';
// [zh] 语句
};

// [zh] 导出模块成员
export const resolveWeatherCityLabel = resolveCityLabel;

// [zh] 导出模块成员
export const resolveCityShort = (label: string) => {
  // [zh] 条件判断 if
  if (!label || label === '…') return label;
  // [zh] return 返回
  return label.length > 3 ? `${label.slice(0, 3)}` : label;
// [zh] 语句
};

// [zh] 导出模块成员
export const resolveWeatherCityShort = resolveCityShort;
