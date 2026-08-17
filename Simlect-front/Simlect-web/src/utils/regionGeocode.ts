import { regionData } from 'element-china-area-data';

type RegionNode = { label: string; value: string; children?: RegionNode[] };

const DIRECT_MUNICIPALITY_KEYS = ['北京', '上海', '天津', '重庆'];

const normalizeKey = (s?: string | null): string => {
  const raw = (s || '').trim().replace(/\s+/g, '');
  if (!raw || raw === '市辖区') return raw;
  return raw.replace(
    /(省|市|自治区|特别行政区|壮族|回族|维吾尔|地区|盟|自治州|自治县|林区|特区|县|区|旗)$/g,
    ''
  );
};

const labelMatch = (label: string, target?: string | null): boolean => {
  if (!target) return false;
  const a = normalizeKey(label);
  const b = normalizeKey(target);
  if (!a || !b) return false;
  return a === b || a.includes(b) || b.includes(a);
};

const isDirectMunicipality = (...names: (string | null | undefined)[]): boolean =>
  names.some((n) => {
    const k = normalizeKey(n);
    return DIRECT_MUNICIPALITY_KEYS.includes(k);
  });

export const matchRegionFromFullAddress = (address: string): string[] | null => {
  const text = (address || '').trim();
  if (!text) return null;

  let best: { codes: string[]; len: number } | null = null;

  const consider = (combo: string, codes: string[]) => {
    if (!combo || !text.includes(combo)) return;
    if (!best || combo.length > best.len) {
      best = { codes, len: combo.length };
    }
  };

  for (const p of regionData as RegionNode[]) {
    const pl = String(p.label || '');
    for (const c of p.children || []) {
      const cl = String(c.label || '');
      const subs = c.children || [];
      for (const d of subs) {
        const dl = String(d.label || '');
        const codes = [String(p.value), String(c.value), String(d.value)];
        consider(pl + cl + dl, codes);
        consider(cl + dl, codes);
        consider(dl, codes);
      }
      consider(pl + cl, [String(p.value), String(c.value)]);
      if (!subs.length) consider(cl, [String(p.value), String(c.value)]);
    }
    consider(pl, [String(p.value)]);
  }

  if (!best) return null;
  return (best as { codes: string[]; len: number }).codes;
};

const resolveMunicipalityCodes = (
  province?: string | null,
  district?: string | null
): string[] | null => {
  for (const p of regionData as RegionNode[]) {
    if (!isDirectMunicipality(p.label, province)) continue;
    const cityLevel =
      (p.children || []).find((c) => labelMatch(c.label, '市辖区')) || p.children?.[0];
    if (!cityLevel) return [String(p.value)];
    if (!district) return [String(p.value), String(cityLevel.value)];
    for (const d of cityLevel.children || []) {
      if (labelMatch(d.label, district)) {
        return [String(p.value), String(cityLevel.value), String(d.value)];
      }
    }
  }
  return null;
};

const findBestByDistrict = (
  province?: string | null,
  city?: string | null,
  district?: string | null
): string[] | null => {
  if (!district) return null;
  let best: { codes: string[]; score: number } | null = null;

  for (const p of regionData as RegionNode[]) {
    for (const c of p.children || []) {
      for (const d of c.children || []) {
        let score = 0;
        if (labelMatch(d.label, district)) score += 40;
        if (city && labelMatch(c.label, city)) score += 20;
        if (province && labelMatch(p.label, province)) score += 20;
        if (score < 40) continue;
        if (!best || score > best.score) {
          best = { codes: [String(p.value), String(c.value), String(d.value)], score };
        }
      }
    }
  }
  if (!best) return null;
  return best.codes;
};

const findByProvinceAndCity = (
  province?: string | null,
  city?: string | null
): string[] | null => {
  if (!city) return null;
  let fallback: string[] | null = null;
  for (const p of regionData as RegionNode[]) {
    for (const c of p.children || []) {
      if (!labelMatch(c.label, city)) continue;
      const sub = c.children || [];
      const codes =
        sub.length === 1
          ? [String(p.value), String(c.value), String(sub[0].value)]
          : [String(p.value), String(c.value)];
      if (province && labelMatch(p.label, province)) return codes;
      if (!fallback) fallback = codes;
    }
  }
  return fallback;
};

export const resolveRegionCodes = (
  province?: string | null,
  city?: string | null,
  district?: string | null
): string[] | null => {
  if (!province && !city && !district) return null;

  const fullText = [province, city, district].filter(Boolean).join('');
  if (fullText) {
    const byText = matchRegionFromFullAddress(fullText);
    if (byText?.length) return byText;
  }

  if (isDirectMunicipality(province, city)) {
    const muni = resolveMunicipalityCodes(province || city, district);
    if (muni?.length) return muni;
  }

  if (district) {
    const byDistrict = findBestByDistrict(province, city, district);
    if (byDistrict?.length) return byDistrict;
  }

  if (city && !district) {
    const byCity = findByProvinceAndCity(province, city);
    if (byCity?.length) return byCity;
  }

  for (const p of regionData as RegionNode[]) {
    const provMatch =
      !province ||
      labelMatch(p.label, province) ||
      labelMatch(p.label, city) ||
      (isDirectMunicipality(province) && isDirectMunicipality(p.label));
    if (!provMatch) continue;

    const children = p.children || [];
    if (!children.length) return [String(p.value)];

    for (const c of children) {
      const cityMatch =
        !city ||
        labelMatch(c.label, city) ||
        (isDirectMunicipality(province, city) && labelMatch(c.label, '市辖区'));

      if (!cityMatch) continue;

      const sub = c.children || [];
      if (district && sub.length) {
        for (const d of sub) {
          if (labelMatch(d.label, district)) {
            return [String(p.value), String(c.value), String(d.value)];
          }
        }
      }
      if (sub.length === 1) {
        return [String(p.value), String(c.value), String(sub[0].value)];
      }
      if (sub.length) return [String(p.value), String(c.value)];
      return [String(p.value), String(c.value)];
    }

    if (district) {
      for (const c of children) {
        for (const d of c.children || []) {
          if (labelMatch(d.label, district)) {
            return [String(p.value), String(c.value), String(d.value)];
          }
        }
      }
    }
  }

  return null;
};
