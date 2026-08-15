// [zh] 导入 `@/api/modules`
import { productApi } from '@/api/modules';
// [zh] 导入 `@/utils/product`
import { filterOnSaleProducts } from '@/utils/product';
// [zh] 导入 `@/utils/image`
import { pickProductCover, resolveImageUrl } from '@/utils/image';

// [zh] 导出模块成员
export type HomeBootstrapData = {
  // [zh] 语句
  hotProducts: any[];
  // [zh] 语句
  products: any[];
  // [zh] 语句
  feedPageNo: number;
  // [zh] 语句
  feedPageTotal: number;
  // [zh] 语句
  feedFinished: boolean;
// [zh] 语句
};

// [zh] 声明变量 `MAX_PRODUCTS`
const MAX_PRODUCTS = 120;

// [zh] 声明变量 `inflight`
let inflight: Promise<HomeBootstrapData> | null = null;
// [zh] 声明变量 `cached`
let cached: HomeBootstrapData | null = null;

// [zh] 声明变量 `loadFeedPage`
const loadFeedPage = async (state: HomeBootstrapData) => {
  // [zh] 声明变量 `next`
  const next = state.feedPageNo + 1;
  // [zh] 声明变量 `page`
  const page = await productApi.loadProduct({ pageNo: next });
  // [zh] 声明变量 `chunk`
  const chunk = filterOnSaleProducts(page?.list);

  // [zh] 条件判断 if
  if (chunk.length > 0) {
    // [zh] 声明变量 `existingIds`
    const existingIds = new Set(state.products.map((p) => p.productId));
    // [zh] 语句
    state.products = state.products.concat(chunk.filter((p) => !existingIds.has(p.productId)));
  // [zh] 语法符号
  }

  // [zh] 语句
  state.feedPageNo = Number(page?.pageNo) || next;
  // [zh] 语句
  state.feedPageTotal = Number(page?.pageTotal) || state.feedPageNo;
  // [zh] 声明变量 `hasMore`
  const hasMore = chunk.length > 0 && state.feedPageNo < state.feedPageTotal;
  // [zh] 语句
  state.feedFinished = !hasMore;
// [zh] 语句
};


// [zh] 导出模块成员
export function prefetchHomeBootstrap(deep = true): Promise<HomeBootstrapData> {
  // [zh] 条件判断 if
  if (cached) return Promise.resolve(cached);
  // [zh] 条件判断 if
  if (inflight) return inflight;

  // [zh] 代码块开始
  inflight = (async () => {
    // [zh] 声明变量 `state`
    const state: HomeBootstrapData = {
      // [zh] TypeScript/JS 语句：`hotProducts: [],`
      hotProducts: [],
      // [zh] TypeScript/JS 语句：`products: [],`
      products: [],
      // [zh] TypeScript/JS 语句：`feedPageNo: 0,`
      feedPageNo: 0,
      // [zh] TypeScript/JS 语句：`feedPageTotal: 1,`
      feedPageTotal: 1,
      // [zh] TypeScript/JS 语句：`feedFinished: false`
      feedFinished: false
    // [zh] 语句
    };

    // [zh] 声明变量 `commend`
    const commend = await productApi.loadCommendProduct();
    // [zh] TypeScript/JS 语句：`state.hotProducts = filterOnSaleProducts(`
    state.hotProducts = filterOnSaleProducts(
      // [zh] TypeScript/JS 语句：`Array.isArray(commend) ? commend : commend?.list`
      Array.isArray(commend) ? commend : commend?.list
    // [zh] 语句
    );

    // [zh] await 等待 Promise（类比 CompletableFuture.get）
    await loadFeedPage(state);

    // [zh] 条件判断 if
    if (deep) {
      // [zh] 代码块开始
      while (!state.feedFinished && state.products.length < MAX_PRODUCTS) {
        // [zh] await 等待 Promise（类比 CompletableFuture.get）
        await loadFeedPage(state);
      // [zh] 语法符号
      }
    // [zh] 语法符号
    }

    // [zh] 语句
    cached = state;
    // [zh] return 返回
    return state;
  // [zh] TypeScript/JS 语句：`})()`
  })()
    // [zh] 代码块开始
    .catch((err) => {
      // [zh] 语句
      inflight = null;
      // [zh] 语句
      throw err;
    // [zh] TypeScript/JS 语句：`})`
    })
    // [zh] 代码块开始
    .then((data) => {
      // [zh] 语句
      inflight = null;
      // [zh] return 返回
      return data;
    // [zh] 语句
    });

  // [zh] return 返回
  return inflight;
// [zh] 语法符号
}


// [zh] 导出模块成员
export function takeHomeBootstrap(): HomeBootstrapData | null {
  // [zh] 声明变量 `data`
  const data = cached;
  // [zh] 语句
  cached = null;
  // [zh] return 返回
  return data;
// [zh] 语法符号
}

// [zh] 导出模块成员
export function clearHomeBootstrap() {
  // [zh] 语句
  cached = null;
  // [zh] 语句
  inflight = null;
// [zh] 语法符号
}


// [zh] 导出模块成员
export function collectHomeVisibleImageUrls(data: HomeBootstrapData, max = 12): string[] {
  // [zh] 声明变量 `urls`
  const urls = new Set<string>();
  // [zh] 声明变量 `add`
  const add = (product: Record<string, any>) => {
    // [zh] 声明变量 `url`
    const url = resolveImageUrl(pickProductCover(product), { useThumbnail: true });
    // [zh] 条件判断 if
    if (url) urls.add(url);
  // [zh] 语句
  };

  // [zh] 语句
  data.hotProducts.slice(0, 4).forEach(add);
  // [zh] for 循环
  for (const product of data.products) {
    // [zh] 条件判断 if
    if (urls.size >= max) break;
    // [zh] 语句
    add(product);
  // [zh] 语法符号
  }

  // [zh] return 返回
  return [...urls];
// [zh] 语法符号
}

// [zh] 声明变量 `preloadOneImage`
const preloadOneImage = (url: string, timeoutMs: number) =>
  // [zh] 代码块开始
  new Promise<void>((resolve) => {
    // [zh] 声明变量 `img`
    const img = new Image();
    // [zh] 声明变量 `settled`
    let settled = false;
    // [zh] 声明变量 `finish`
    const finish = () => {
      // [zh] 条件判断 if
      if (settled) return;
      // [zh] 语句
      settled = true;
      // [zh] 语句
      window.clearTimeout(timer);
      // [zh] 语句
      img.onload = null;
      // [zh] 语句
      img.onerror = null;
      // [zh] 语句
      resolve();
    // [zh] 语句
    };
    // [zh] 声明变量 `timer`
    const timer = window.setTimeout(finish, timeoutMs);
    // [zh] 语句
    img.onload = finish;
    // [zh] 语句
    img.onerror = finish;
    // [zh] 语句
    img.src = url;
  // [zh] 语句
  });


// [zh] 导出模块成员
export async function preloadHomeVisibleImages(
  // [zh] TypeScript/JS 语句：`data: HomeBootstrapData,`
  data: HomeBootstrapData,
  // [zh] TypeScript/JS 语句：`opts?: { maxImages?: number; perImageTimeoutMs?: number`
  opts?: { maxImages?: number; perImageTimeoutMs?: number; totalTimeoutMs?: number }
// [zh] 代码块开始
) {
  // [zh] 声明变量 `urls`
  const urls = collectHomeVisibleImageUrls(data, opts?.maxImages ?? 12);
  // [zh] 条件判断 if
  if (!urls.length) return;

  // [zh] 声明变量 `perImageTimeoutMs`
  const perImageTimeoutMs = opts?.perImageTimeoutMs ?? 10000;
  // [zh] 声明变量 `totalTimeoutMs`
  const totalTimeoutMs = opts?.totalTimeoutMs ?? 18000;

  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await Promise.race([
    // [zh] TypeScript/JS 语句：`Promise.all(urls.map((url) => preloadOneImage(url, perI`
    Promise.all(urls.map((url) => preloadOneImage(url, perImageTimeoutMs))),
    // [zh] TypeScript/JS 语句：`new Promise<void>((resolve) => window.setTimeout(resolv`
    new Promise<void>((resolve) => window.setTimeout(resolve, totalTimeoutMs))
  // [zh] 语句
  ]);
// [zh] 语法符号
}


// [zh] 导出模块成员
export async function prepareHomeForSplash(deep = false): Promise<HomeBootstrapData> {
  // [zh] 声明变量 `data`
  const data = await prefetchHomeBootstrap(deep);
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await preloadHomeVisibleImages(data);
  // [zh] return 返回
  return data;
// [zh] 语法符号
}

// [zh] 声明变量 `resolveHomeSplashPaint`
let resolveHomeSplashPaint: (() => void) | null = null;


// [zh] 导出模块成员
export function createHomeSplashPaintGate(): Promise<void> {
  // [zh] return 返回
  return new Promise<void>((resolve) => {
    // [zh] 语句
    resolveHomeSplashPaint = resolve;
  // [zh] 语句
  });
// [zh] 语法符号
}

// [zh] 导出模块成员
export function signalHomeSplashPaintReady() {
  // [zh] 语句
  resolveHomeSplashPaint?.();
  // [zh] 语句
  resolveHomeSplashPaint = null;
// [zh] 语法符号
}

// [zh] 声明变量 `waitForImageElement`
const waitForImageElement = (img: HTMLImageElement, timeoutMs: number) =>
  // [zh] 代码块开始
  new Promise<void>((resolve) => {
    // [zh] 条件判断 if
    if (img.complete && img.naturalWidth > 0) {
      // [zh] 语句
      resolve();
      // [zh] 语句
      return;
    // [zh] 语法符号
    }
    // [zh] 声明变量 `settled`
    let settled = false;
    // [zh] 声明变量 `finish`
    const finish = () => {
      // [zh] 条件判断 if
      if (settled) return;
      // [zh] 语句
      settled = true;
      // [zh] 语句
      window.clearTimeout(timer);
      // [zh] 语句
      img.onload = null;
      // [zh] 语句
      img.onerror = null;
      // [zh] 语句
      resolve();
    // [zh] 语句
    };
    // [zh] 声明变量 `timer`
    const timer = window.setTimeout(finish, timeoutMs);
    // [zh] 语句
    img.onload = finish;
    // [zh] 语句
    img.onerror = finish;
  // [zh] 语句
  });

// [zh] 声明变量 `nextFrame`
const nextFrame = () =>
  // [zh] 语句
  new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));


// [zh] 导出模块成员
export async function waitForHomeVisibleImagesInDom(
  // [zh] TypeScript/JS 语句：`root: ParentNode | null | undefined,`
  root: ParentNode | null | undefined,
  // [zh] TypeScript/JS 语句：`opts?: { maxImages?: number; perImageTimeoutMs?: number`
  opts?: { maxImages?: number; perImageTimeoutMs?: number; totalTimeoutMs?: number }
// [zh] 代码块开始
) {
  // [zh] 条件判断 if
  if (!root) return;

  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await nextFrame();
  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await nextFrame();

  // [zh] 声明变量 `maxImages`
  const maxImages = opts?.maxImages ?? 10;
  // [zh] 声明变量 `perImageTimeoutMs`
  const perImageTimeoutMs = opts?.perImageTimeoutMs ?? 8000;
  // [zh] 声明变量 `totalTimeoutMs`
  const totalTimeoutMs = opts?.totalTimeoutMs ?? 15000;

  // [zh] 声明变量 `imgs`
  const imgs = [
    // [zh] TypeScript/JS 语句：`...root.querySelectorAll<HTMLImageElement>(`
    ...root.querySelectorAll<HTMLImageElement>(
      // [zh] TypeScript/JS 语句：`'.editor-img-wrap img, .simlect-waterfall .product-imag`
      '.editor-img-wrap img, .simlect-waterfall .product-image img'
    // [zh] 语法符号
    )
  // [zh] 语句
  ].slice(0, maxImages);

  // [zh] 条件判断 if
  if (!imgs.length) return;

  // [zh] await 等待 Promise（类比 CompletableFuture.get）
  await Promise.race([
    // [zh] TypeScript/JS 语句：`Promise.all(imgs.map((img) => waitForImageElement(img, `
    Promise.all(imgs.map((img) => waitForImageElement(img, perImageTimeoutMs))),
    // [zh] TypeScript/JS 语句：`new Promise<void>((resolve) => window.setTimeout(resolv`
    new Promise<void>((resolve) => window.setTimeout(resolve, totalTimeoutMs))
  // [zh] 语句
  ]);
// [zh] 语法符号
}
