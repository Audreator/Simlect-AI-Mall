
// [zh] 导出模块成员
export const MAX_UPLOAD_BYTES = 5 * 1024 * 1024;

// [zh] 声明变量 `loadImageFromBlob`
const loadImageFromBlob = (blob: Blob): Promise<HTMLImageElement> =>
  // [zh] 代码块开始
  new Promise((resolve, reject) => {
    // [zh] 声明变量 `url`
    const url = URL.createObjectURL(blob);
    // [zh] 声明变量 `img`
    const img = new Image();
    // [zh] 代码块开始
    img.onload = () => {
      // [zh] 语句
      URL.revokeObjectURL(url);
      // [zh] 语句
      resolve(img);
    // [zh] 语句
    };
    // [zh] 代码块开始
    img.onerror = () => {
      // [zh] 语句
      URL.revokeObjectURL(url);
      // [zh] 语句
      reject(new Error('图片解码失败'));
    // [zh] 语句
    };
    // [zh] 语句
    img.src = url;
  // [zh] 语句
  });

// [zh] 声明变量 `canvasToBlob`
const canvasToBlob = (canvas: HTMLCanvasElement, type: string, quality?: number): Promise<Blob> =>
  // [zh] 代码块开始
  new Promise((resolve, reject) => {
    // [zh] TypeScript/JS 语句：`canvas.toBlob(`
    canvas.toBlob(
      // [zh] TypeScript/JS 语句：`(blob) => (blob ? resolve(blob) : reject(new Error('图片导`
      (blob) => (blob ? resolve(blob) : reject(new Error('图片导出失败'))),
      // [zh] TypeScript/JS 语句：`type,`
      type,
      // [zh] TypeScript/JS 语句：`quality`
      quality
    // [zh] 语句
    );
  // [zh] 语句
  });

// [zh] 声明变量 `drawScaled`
const drawScaled = (img: HTMLImageElement, maxEdge: number) => {
  // [zh] 声明变量 `w`
  let w = img.width;
  // [zh] 声明变量 `h`
  let h = img.height;
  // [zh] 声明变量 `scale`
  const scale = Math.min(1, maxEdge / Math.max(w, h, 1));
  // [zh] 语句
  w = Math.max(1, Math.round(w * scale));
  // [zh] 语句
  h = Math.max(1, Math.round(h * scale));
  // [zh] 声明变量 `canvas`
  const canvas = document.createElement('canvas');
  // [zh] 语句
  canvas.width = w;
  // [zh] 语句
  canvas.height = h;
  // [zh] 声明变量 `ctx`
  const ctx = canvas.getContext('2d');
  // [zh] 条件判断 if
  if (!ctx) throw new Error('无法创建画布');
  // [zh] 语句
  ctx.drawImage(img, 0, 0, w, h);
  // [zh] return 返回
  return { canvas, ctx, w, h };
// [zh] 语句
};

// [zh] 声明变量 `encodeUnderLimit`
const encodeUnderLimit = async (
  // [zh] TypeScript/JS 语句：`canvas: HTMLCanvasElement,`
  canvas: HTMLCanvasElement,
  // [zh] TypeScript/JS 语句：`maxBytes: number,`
  maxBytes: number,
  // [zh] TypeScript/JS 语句：`preferPng: boolean`
  preferPng: boolean
// [zh] 代码块开始
): Promise<Blob> => {
  // [zh] 条件判断 if
  if (preferPng) {
    // [zh] 声明变量 `png`
    const png = await canvasToBlob(canvas, 'image/png');
    // [zh] 条件判断 if
    if (png.size <= maxBytes) return png;
  // [zh] 语法符号
  }

  // [zh] 声明变量 `quality`
  let quality = 0.92;
  // [zh] 声明变量 `blob`
  let blob = await canvasToBlob(canvas, 'image/jpeg', quality);
  // [zh] 代码块开始
  while (blob.size > maxBytes && quality > 0.35) {
    // [zh] 语句
    quality -= 0.07;
    // [zh] 语句
    blob = await canvasToBlob(canvas, 'image/jpeg', quality);
  // [zh] 语法符号
  }
  // [zh] return 返回
  return blob;
// [zh] 语句
};

// [zh] 导出模块成员
export async function prepareImageForUpload(
  // [zh] TypeScript/JS 语句：`input: File | Blob,`
  input: File | Blob,
  // [zh] TypeScript/JS 语句：`opts?: { maxBytes?: number; maxEdge?: number; preferPng`
  opts?: { maxBytes?: number; maxEdge?: number; preferPng?: boolean }
// [zh] 代码块开始
): Promise<Blob> {
  // [zh] 声明变量 `maxBytes`
  const maxBytes = opts?.maxBytes ?? MAX_UPLOAD_BYTES;
  // [zh] 声明变量 `maxEdge`
  const maxEdge = opts?.maxEdge ?? 4096;
  // [zh] 声明变量 `preferPng`
  const preferPng = opts?.preferPng ?? false;

  // [zh] 声明变量 `img`
  const img = await loadImageFromBlob(input);
  // [zh] 语句
  let { canvas, w, h } = drawScaled(img, maxEdge);
  // [zh] 声明变量 `blob`
  let blob = await encodeUnderLimit(canvas, maxBytes, preferPng && input.size <= maxBytes);

  // [zh] 条件判断 if
  if (blob.size <= maxBytes) return blob;

  // [zh] 声明变量 `shrink`
  let shrink = 0.85;
  // [zh] 代码块开始
  while (blob.size > maxBytes && shrink >= 0.35) {
    // [zh] 语句
    w = Math.max(1, Math.round(w * shrink));
    // [zh] 语句
    h = Math.max(1, Math.round(h * shrink));
    // [zh] 语句
    canvas.width = w;
    // [zh] 语句
    canvas.height = h;
    // [zh] 声明变量 `ctx`
    const ctx = canvas.getContext('2d');
    // [zh] 条件判断 if
    if (!ctx) break;
    // [zh] 语句
    ctx.drawImage(img, 0, 0, w, h);
    // [zh] 语句
    blob = await encodeUnderLimit(canvas, maxBytes, false);
    // [zh] 语句
    shrink -= 0.1;
  // [zh] 语法符号
  }

  // [zh] 条件判断 if
  if (blob.size > maxBytes) {
    // [zh] 语句
    throw new Error('图片过大，无法压缩到5MB以内，请换一张更小的图片');
  // [zh] 语法符号
  }
  // [zh] return 返回
  return blob;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function dataUrlToBlob(dataUrl: string, mime: string): Blob {
  // [zh] 声明变量 `parts`
  const parts = dataUrl.split(',');
  // [zh] 条件判断 if
  if (parts.length !== 2 || !parts[1]) {
    // [zh] 语句
    throw new Error('图片导出失败');
  // [zh] 语法符号
  }
  // [zh] 声明变量 `byteString`
  const byteString = atob(parts[1]);
  // [zh] 条件判断 if
  if (!byteString || byteString.length === 0) {
    // [zh] 语句
    throw new Error('图片导出失败');
  // [zh] 语法符号
  }
  // [zh] 声明变量 `ab`
  const ab = new ArrayBuffer(byteString.length);
  // [zh] 声明变量 `ia`
  const ia = new Uint8Array(ab);
  // [zh] for 循环
  for (let i = 0; i < byteString.length; i++) {
    // [zh] 语句
    ia[i] = byteString.charCodeAt(i);
  // [zh] 语法符号
  }
  // [zh] return 返回
  return new Blob([ab], { type: mime });
// [zh] 语法符号
}

// [zh] 导出模块成员
export async function canvasToJpegBlob(canvas: HTMLCanvasElement, quality = 0.9): Promise<Blob> {
  // [zh] 声明变量 `target`
  let target = canvas;
  // [zh] 声明变量 `w`
  let w = canvas.width;
  // [zh] 声明变量 `h`
  let h = canvas.height;
  // [zh] 声明变量 `minEdge`
  const minEdge = Math.min(w, h);
  // [zh] 条件判断 if
  if (minEdge < 128) {
    // [zh] 声明变量 `scale`
    const scale = 128 / minEdge;
    // [zh] 语句
    w = Math.max(128, Math.round(w * scale));
    // [zh] 语句
    h = Math.max(128, Math.round(h * scale));
    // [zh] 声明变量 `scaled`
    const scaled = document.createElement('canvas');
    // [zh] 语句
    scaled.width = w;
    // [zh] 语句
    scaled.height = h;
    // [zh] 声明变量 `ctx`
    const ctx = scaled.getContext('2d');
    // [zh] 条件判断 if
    if (!ctx) throw new Error('无法创建画布');
    // [zh] 语句
    ctx.drawImage(canvas, 0, 0, w, h);
    // [zh] 语句
    target = scaled;
  // [zh] 语法符号
  }

  // [zh] 声明变量 `blob`
  const blob = await new Promise<Blob | null>((resolve) => {
    // [zh] 语句
    target.toBlob((b) => resolve(b), 'image/jpeg', quality);
  // [zh] 语句
  });
  // [zh] 条件判断 if
  if (blob && blob.size > 1024) {
    // [zh] return 返回
    return new Blob([await blob.arrayBuffer()], { type: 'image/jpeg' });
  // [zh] 语法符号
  }
  // [zh] return 返回
  return dataUrlToBlob(target.toDataURL('image/jpeg', quality), 'image/jpeg');
// [zh] 语法符号
}

// [zh] 导出模块成员
export function isServerTranscodeFormat(input: File | Blob): boolean {
  // [zh] 声明变量 `type`
  const type = (input.type || '').toLowerCase();
  // [zh] 声明变量 `name`
  const name = input instanceof File ? input.name.toLowerCase() : '';
  // [zh] return 返回
  return (
    // [zh] TypeScript/JS 语句：`/heic|heif/.test(type) ||`
    /heic|heif/.test(type) ||
    // [zh] TypeScript/JS 语句：`/\.heic$|\.heif$|\.heics$|\.heifs$/i.test(name)`
    /\.heic$|\.heif$|\.heics$|\.heifs$/i.test(name)
  // [zh] 语句
  );
// [zh] 语法符号
}

// [zh] 导出模块成员
export async function prepareForUpload(input: File | Blob): Promise<Blob> {
  // [zh] 条件判断 if
  if (isServerTranscodeFormat(input)) {
    // [zh] return 返回
    return input;
  // [zh] 语法符号
  }
  // [zh] return 返回
  return prepareImageForUpload(input);
// [zh] 语法符号
}

// [zh] 导出模块成员
export async function resolveCommentUploadBlob(
  // [zh] TypeScript/JS 语句：`file: File,`
  file: File,
  // [zh] TypeScript/JS 语句：`openEditor: (file: File) => Promise<Blob>`
  openEditor: (file: File) => Promise<Blob>
// [zh] 代码块开始
): Promise<Blob> {
  // [zh] 条件判断 if
  if (isServerTranscodeFormat(file)) {
    // [zh] return 返回
    return file;
  // [zh] 语法符号
  }
  // [zh] 代码块开始
  try {
    // [zh] return 返回
    return await openEditor(file);
  // [zh] 代码块开始
  } catch {
    // [zh] return 返回
    return file;
  // [zh] 语法符号
  }
// [zh] 语法符号
}
