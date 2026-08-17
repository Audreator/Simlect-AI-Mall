
export function resolveAgentWsUrl(): string {
  const raw = (import.meta.env.VITE_WS as string | undefined)?.trim() || '/ws/';
  const pageHost = window.location.hostname;
  const pageProtocol = window.location.protocol;

  if (/^wss?:\/\//i.test(raw)) {
    try {
      const parsed = new URL(raw.replace(/\?token=.*$/i, ''));
      const envHost = parsed.hostname;
      if (
        (envHost === 'localhost' || envHost === '127.0.0.1') &&
        pageHost !== 'localhost' &&
        pageHost !== '127.0.0.1'
      ) {
        const protocol = pageProtocol === 'https:' ? 'wss:' : 'ws:';
        return `${protocol}//${window.location.host}/ws/`;
      }
    } catch {

    }
    return raw.replace(/\?token=.*$/i, '').replace(/\?$/, '') || raw;
  }

  const protocol = pageProtocol === 'https:' ? 'wss:' : 'ws:';
  const path = raw.startsWith('/') ? raw.replace(/\?token=.*$/i, '') : '/ws/';
  return `${protocol}//${window.location.host}${path}`;
}
