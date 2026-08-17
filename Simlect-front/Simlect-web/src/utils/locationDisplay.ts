import type { LocationPayload } from '@/api/location';

export const simplifyCityName = (name?: string | null) => {
  if (!name) return '';
  return String(name)
    .trim()
    .replace(/(特别行政区|自治区|自治州|地区|盟|省|市|区|县)$/g, '')
    .trim();
};

export const parseCityFromSummary = (summary?: string | null) => {
  if (!summary) return '';
  const first = summary.trim().split(/\s+/)[0];
  if (!first || first === '当地' || first === '—') return '';
  return first;
};

export const resolveCityLabel = (data?: LocationPayload | null, loading = false): string => {
  if (!data) return loading ? '…' : '';

  const raw =
    data.city ||
    data.district ||
    data.province ||
    parseCityFromSummary(data.summary) ||
    '';

  const label = simplifyCityName(raw);
  if (label) return label;

  const fromSummary = simplifyCityName(parseCityFromSummary(data.summary));
  if (fromSummary) return fromSummary;

  return loading ? '…' : '';
};

export const resolveWeatherCityLabel = resolveCityLabel;

export const resolveCityShort = (label: string) => {
  if (!label || label === '…') return label;
  return label.length > 3 ? `${label.slice(0, 3)}` : label;
};

export const resolveWeatherCityShort = resolveCityShort;
