import { productApi } from '@/api/modules';
import { filterOnSaleProducts } from '@/utils/product';
import { pickProductCover, resolveImageUrl } from '@/utils/image';

export type HomeBootstrapData = {
  hotProducts: any[];
  products: any[];
  feedPageNo: number;
  feedPageTotal: number;
  feedFinished: boolean;
};

const MAX_PRODUCTS = 120;

let inflight: Promise<HomeBootstrapData> | null = null;
let cached: HomeBootstrapData | null = null;

const loadFeedPage = async (state: HomeBootstrapData) => {
  const next = state.feedPageNo + 1;
  const page = await productApi.loadProduct({ pageNo: next });
  const chunk = filterOnSaleProducts(page?.list);

  if (chunk.length > 0) {
    const existingIds = new Set(state.products.map((p) => p.productId));
    state.products = state.products.concat(chunk.filter((p) => !existingIds.has(p.productId)));
  }

  state.feedPageNo = Number(page?.pageNo) || next;
  state.feedPageTotal = Number(page?.pageTotal) || state.feedPageNo;
  const hasMore = chunk.length > 0 && state.feedPageNo < state.feedPageTotal;
  state.feedFinished = !hasMore;
};


export function prefetchHomeBootstrap(deep = true): Promise<HomeBootstrapData> {
  if (cached) return Promise.resolve(cached);
  if (inflight) return inflight;

  inflight = (async () => {
    const state: HomeBootstrapData = {
      hotProducts: [],
      products: [],
      feedPageNo: 0,
      feedPageTotal: 1,
      feedFinished: false
    };

    const commend = await productApi.loadCommendProduct();
    state.hotProducts = filterOnSaleProducts(
      Array.isArray(commend) ? commend : commend?.list
    );

    await loadFeedPage(state);

    if (deep) {
      while (!state.feedFinished && state.products.length < MAX_PRODUCTS) {
        await loadFeedPage(state);
      }
    }

    cached = state;
    return state;
  })()
    .catch((err) => {
      inflight = null;
      throw err;
    })
    .then((data) => {
      inflight = null;
      return data;
    });

  return inflight;
}


export function takeHomeBootstrap(): HomeBootstrapData | null {
  const data = cached;
  cached = null;
  return data;
}

export function clearHomeBootstrap() {
  cached = null;
  inflight = null;
}


export function collectHomeVisibleImageUrls(data: HomeBootstrapData, max = 12): string[] {
  const urls = new Set<string>();
  const add = (product: Record<string, any>) => {
    const url = resolveImageUrl(pickProductCover(product), { useThumbnail: true });
    if (url) urls.add(url);
  };

  data.hotProducts.slice(0, 4).forEach(add);
  for (const product of data.products) {
    if (urls.size >= max) break;
    add(product);
  }

  return [...urls];
}

const preloadOneImage = (url: string, timeoutMs: number) =>
  new Promise<void>((resolve) => {
    const img = new Image();
    let settled = false;
    const finish = () => {
      if (settled) return;
      settled = true;
      window.clearTimeout(timer);
      img.onload = null;
      img.onerror = null;
      resolve();
    };
    const timer = window.setTimeout(finish, timeoutMs);
    img.onload = finish;
    img.onerror = finish;
    img.src = url;
  });


export async function preloadHomeVisibleImages(
  data: HomeBootstrapData,
  opts?: { maxImages?: number; perImageTimeoutMs?: number; totalTimeoutMs?: number }
) {
  const urls = collectHomeVisibleImageUrls(data, opts?.maxImages ?? 12);
  if (!urls.length) return;

  const perImageTimeoutMs = opts?.perImageTimeoutMs ?? 10000;
  const totalTimeoutMs = opts?.totalTimeoutMs ?? 18000;

  await Promise.race([
    Promise.all(urls.map((url) => preloadOneImage(url, perImageTimeoutMs))),
    new Promise<void>((resolve) => window.setTimeout(resolve, totalTimeoutMs))
  ]);
}


export async function prepareHomeForSplash(deep = false): Promise<HomeBootstrapData> {
  const data = await prefetchHomeBootstrap(deep);
  await preloadHomeVisibleImages(data);
  return data;
}

let resolveHomeSplashPaint: (() => void) | null = null;


export function createHomeSplashPaintGate(): Promise<void> {
  return new Promise<void>((resolve) => {
    resolveHomeSplashPaint = resolve;
  });
}

export function signalHomeSplashPaintReady() {
  resolveHomeSplashPaint?.();
  resolveHomeSplashPaint = null;
}

const waitForImageElement = (img: HTMLImageElement, timeoutMs: number) =>
  new Promise<void>((resolve) => {
    if (img.complete && img.naturalWidth > 0) {
      resolve();
      return;
    }
    let settled = false;
    const finish = () => {
      if (settled) return;
      settled = true;
      window.clearTimeout(timer);
      img.onload = null;
      img.onerror = null;
      resolve();
    };
    const timer = window.setTimeout(finish, timeoutMs);
    img.onload = finish;
    img.onerror = finish;
  });

const nextFrame = () =>
  new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));


export async function waitForHomeVisibleImagesInDom(
  root: ParentNode | null | undefined,
  opts?: { maxImages?: number; perImageTimeoutMs?: number; totalTimeoutMs?: number }
) {
  if (!root) return;

  await nextFrame();
  await nextFrame();

  const maxImages = opts?.maxImages ?? 10;
  const perImageTimeoutMs = opts?.perImageTimeoutMs ?? 8000;
  const totalTimeoutMs = opts?.totalTimeoutMs ?? 15000;

  const imgs = [
    ...root.querySelectorAll<HTMLImageElement>(
      '.editor-img-wrap img, .simlect-waterfall .product-image img'
    )
  ].slice(0, maxImages);

  if (!imgs.length) return;

  await Promise.race([
    Promise.all(imgs.map((img) => waitForImageElement(img, perImageTimeoutMs))),
    new Promise<void>((resolve) => window.setTimeout(resolve, totalTimeoutMs))
  ]);
}
