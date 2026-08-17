
export const MAX_UPLOAD_BYTES = 5 * 1024 * 1024;

const loadImageFromBlob = (blob: Blob): Promise<HTMLImageElement> =>
  new Promise((resolve, reject) => {
    const url = URL.createObjectURL(blob);
    const img = new Image();
    img.onload = () => {
      URL.revokeObjectURL(url);
      resolve(img);
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('图片解码失败'));
    };
    img.src = url;
  });

const canvasToBlob = (canvas: HTMLCanvasElement, type: string, quality?: number): Promise<Blob> =>
  new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => (blob ? resolve(blob) : reject(new Error('图片导出失败'))),
      type,
      quality
    );
  });

const drawScaled = (img: HTMLImageElement, maxEdge: number) => {
  let w = img.width;
  let h = img.height;
  const scale = Math.min(1, maxEdge / Math.max(w, h, 1));
  w = Math.max(1, Math.round(w * scale));
  h = Math.max(1, Math.round(h * scale));
  const canvas = document.createElement('canvas');
  canvas.width = w;
  canvas.height = h;
  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('无法创建画布');
  ctx.drawImage(img, 0, 0, w, h);
  return { canvas, ctx, w, h };
};

const encodeUnderLimit = async (
  canvas: HTMLCanvasElement,
  maxBytes: number,
  preferPng: boolean
): Promise<Blob> => {
  if (preferPng) {
    const png = await canvasToBlob(canvas, 'image/png');
    if (png.size <= maxBytes) return png;
  }

  let quality = 0.92;
  let blob = await canvasToBlob(canvas, 'image/jpeg', quality);
  while (blob.size > maxBytes && quality > 0.35) {
    quality -= 0.07;
    blob = await canvasToBlob(canvas, 'image/jpeg', quality);
  }
  return blob;
};

export async function prepareImageForUpload(
  input: File | Blob,
  opts?: { maxBytes?: number; maxEdge?: number; preferPng?: boolean }
): Promise<Blob> {
  const maxBytes = opts?.maxBytes ?? MAX_UPLOAD_BYTES;
  const maxEdge = opts?.maxEdge ?? 4096;
  const preferPng = opts?.preferPng ?? false;

  const img = await loadImageFromBlob(input);
  let { canvas, w, h } = drawScaled(img, maxEdge);
  let blob = await encodeUnderLimit(canvas, maxBytes, preferPng && input.size <= maxBytes);

  if (blob.size <= maxBytes) return blob;

  let shrink = 0.85;
  while (blob.size > maxBytes && shrink >= 0.35) {
    w = Math.max(1, Math.round(w * shrink));
    h = Math.max(1, Math.round(h * shrink));
    canvas.width = w;
    canvas.height = h;
    const ctx = canvas.getContext('2d');
    if (!ctx) break;
    ctx.drawImage(img, 0, 0, w, h);
    blob = await encodeUnderLimit(canvas, maxBytes, false);
    shrink -= 0.1;
  }

  if (blob.size > maxBytes) {
    throw new Error('图片过大，无法压缩到5MB以内，请换一张更小的图片');
  }
  return blob;
}

export function dataUrlToBlob(dataUrl: string, mime: string): Blob {
  const parts = dataUrl.split(',');
  if (parts.length !== 2 || !parts[1]) {
    throw new Error('图片导出失败');
  }
  const byteString = atob(parts[1]);
  if (!byteString || byteString.length === 0) {
    throw new Error('图片导出失败');
  }
  const ab = new ArrayBuffer(byteString.length);
  const ia = new Uint8Array(ab);
  for (let i = 0; i < byteString.length; i++) {
    ia[i] = byteString.charCodeAt(i);
  }
  return new Blob([ab], { type: mime });
}

export async function canvasToJpegBlob(canvas: HTMLCanvasElement, quality = 0.9): Promise<Blob> {
  let target = canvas;
  let w = canvas.width;
  let h = canvas.height;
  const minEdge = Math.min(w, h);
  if (minEdge < 128) {
    const scale = 128 / minEdge;
    w = Math.max(128, Math.round(w * scale));
    h = Math.max(128, Math.round(h * scale));
    const scaled = document.createElement('canvas');
    scaled.width = w;
    scaled.height = h;
    const ctx = scaled.getContext('2d');
    if (!ctx) throw new Error('无法创建画布');
    ctx.drawImage(canvas, 0, 0, w, h);
    target = scaled;
  }

  const blob = await new Promise<Blob | null>((resolve) => {
    target.toBlob((b) => resolve(b), 'image/jpeg', quality);
  });
  if (blob && blob.size > 1024) {
    return new Blob([await blob.arrayBuffer()], { type: 'image/jpeg' });
  }
  return dataUrlToBlob(target.toDataURL('image/jpeg', quality), 'image/jpeg');
}

export function isServerTranscodeFormat(input: File | Blob): boolean {
  const type = (input.type || '').toLowerCase();
  const name = input instanceof File ? input.name.toLowerCase() : '';
  return (
    /heic|heif/.test(type) ||
    /\.heic$|\.heif$|\.heics$|\.heifs$/i.test(name)
  );
}

export async function prepareForUpload(input: File | Blob): Promise<Blob> {
  if (isServerTranscodeFormat(input)) {
    return input;
  }
  return prepareImageForUpload(input);
}

export async function resolveCommentUploadBlob(
  file: File,
  openEditor: (file: File) => Promise<Blob>
): Promise<Blob> {
  if (isServerTranscodeFormat(file)) {
    return file;
  }
  try {
    return await openEditor(file);
  } catch {
    return file;
  }
}
