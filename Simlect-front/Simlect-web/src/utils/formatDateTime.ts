
export function formatDisplayDateTime(val?: string | number | null): string {
  if (val === null || val === undefined || val === '') return '';
  if (typeof val === 'string') {
    const normalized = val.replace('T', ' ').trim();
    if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(normalized)) {
      return normalized;
    }
  }
  const d = new Date(val);
  if (Number.isNaN(d.getTime())) {
    return String(val).replace('T', ' ').slice(0, 19);
  }
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}
