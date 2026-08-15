

// [zh] 导出模块成员
export interface AgentConsultProduct {

  // [zh] 语句
  productId: string;

  // [zh] 语句
  productName: string;

  // [zh] 语句
  cover?: string;

  // [zh] 语句
  minPrice?: number | string;

// [zh] 语法符号
}



// [zh] 声明变量 `CARD_START`
const CARD_START = '<<<PRODUCT_CONSULT>>>';

// [zh] 声明变量 `CARD_END`
const CARD_END = '<<<END_CARD>>>';



// [zh] 导出模块成员
export const AGENT_CONSULT_PRODUCT_KEY = 'eshop_agent_consult_product';





// [zh] 导出模块成员
export function getAgentConsultStorageKey(userId?: string | null): string {

  // [zh] 声明变量 `id`
  const id = userId?.trim();

  // [zh] return 返回
  return id ? `${AGENT_CONSULT_PRODUCT_KEY}:userId:${id}` : AGENT_CONSULT_PRODUCT_KEY;

// [zh] 语法符号
}



// [zh] 导出模块成员
export function buildProductConsultMessage(product: AgentConsultProduct): string {

  // [zh] 声明变量 `payload`
  const payload = JSON.stringify({

    // [zh] TypeScript/JS 语句：`productId: product.productId,`
    productId: product.productId,

    // [zh] TypeScript/JS 语句：`productName: product.productName,`
    productName: product.productName,

    // [zh] TypeScript/JS 语句：`cover: product.cover || '',`
    cover: product.cover || '',

    // [zh] TypeScript/JS 语句：`minPrice: product.minPrice != null && product.minPrice `
    minPrice: product.minPrice != null && product.minPrice !== '' ? Number(product.minPrice) : null

  // [zh] 语句
  });

  // [zh] 声明变量 `line`
  const line = `【商品咨询】我想了解「${product.productName}」（商品编号：${product.productId}）`;

  // [zh] return 返回
  return `${CARD_START}${payload}${CARD_END}\n${line}`;

// [zh] 语法符号
}



// [zh] 导出模块成员
export function parseProductConsultMessage(userMessage?: string | null): {

  // [zh] 语句
  card: AgentConsultProduct | null;

  // [zh] 语句
  text: string;

// [zh] 代码块开始
} {

  // [zh] 条件判断 if
  if (!userMessage) return { card: null, text: '' };

  // [zh] 声明变量 `start`
  const start = userMessage.indexOf(CARD_START);

  // [zh] 条件判断 if
  if (start < 0) return { card: null, text: userMessage };

  // [zh] 声明变量 `end`
  const end = userMessage.indexOf(CARD_END, start + CARD_START.length);

  // [zh] 条件判断 if
  if (end < 0) return { card: null, text: userMessage };



  // [zh] 声明变量 `card`
  let card: AgentConsultProduct | null = null;

  // [zh] 代码块开始
  try {

    // [zh] 声明变量 `obj`
    const obj = JSON.parse(userMessage.slice(start + CARD_START.length, end));

    // [zh] 条件判断 if
    if (obj?.productId && obj?.productName) {

      // [zh] 代码块开始
      card = {

        // [zh] TypeScript/JS 语句：`productId: String(obj.productId),`
        productId: String(obj.productId),

        // [zh] TypeScript/JS 语句：`productName: String(obj.productName),`
        productName: String(obj.productName),

        // [zh] TypeScript/JS 语句：`cover: obj.cover ? String(obj.cover) : undefined,`
        cover: obj.cover ? String(obj.cover) : undefined,

        // [zh] TypeScript/JS 语句：`minPrice: obj.minPrice`
        minPrice: obj.minPrice

      // [zh] 语句
      };

    // [zh] 语法符号
    }

  // [zh] 代码块开始
  } catch {

    

  // [zh] 语法符号
  }



  // [zh] 声明变量 `text`
  let text = userMessage.slice(end + CARD_END.length);

  // [zh] 条件判断 if
  if (text.startsWith('\n')) text = text.slice(1);

  // [zh] return 返回
  return { card, text: text.trim() };

// [zh] 语法符号
}



// [zh] 导出模块成员
export function saveAgentConsultProduct(product: AgentConsultProduct, userId?: string | null) {

  // [zh] 语句
  sessionStorage.setItem(getAgentConsultStorageKey(userId), JSON.stringify(product));

// [zh] 语法符号
}



// [zh] 导出模块成员
export function loadAgentConsultProduct(userId?: string | null): AgentConsultProduct | null {

  // [zh] 声明变量 `raw`
  const raw = sessionStorage.getItem(getAgentConsultStorageKey(userId));

  // [zh] 条件判断 if
  if (!raw) return null;

  // [zh] 代码块开始
  try {

    // [zh] 声明变量 `o`
    const o = JSON.parse(raw);

    // [zh] 条件判断 if
    if (o?.productId && o?.productName) {

      // [zh] return 返回
      return {

        // [zh] TypeScript/JS 语句：`productId: String(o.productId),`
        productId: String(o.productId),

        // [zh] TypeScript/JS 语句：`productName: String(o.productName),`
        productName: String(o.productName),

        // [zh] TypeScript/JS 语句：`cover: o.cover,`
        cover: o.cover,

        // [zh] TypeScript/JS 语句：`minPrice: o.minPrice`
        minPrice: o.minPrice

      // [zh] 语句
      };

    // [zh] 语法符号
    }

  // [zh] 代码块开始
  } catch {

    

  // [zh] 语法符号
  }

  // [zh] return 返回
  return null;

// [zh] 语法符号
}



// [zh] 导出模块成员
export function clearAgentConsultProduct(userId?: string | null) {

  // [zh] 语句
  sessionStorage.removeItem(getAgentConsultStorageKey(userId));

// [zh] 语法符号
}


