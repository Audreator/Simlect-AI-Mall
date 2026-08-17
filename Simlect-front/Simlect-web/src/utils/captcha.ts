
export function formatCaptchaSrc(checkCode?: string): string {
  if (!checkCode) return '';
  if (checkCode.startsWith('data:')) return checkCode;
  return `data:image/png;base64,${checkCode}`;
}

export interface CheckCodeResult {
  checkCode: string;
  checkCodeKey: string;
}
