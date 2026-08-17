

export interface AgentConsultProduct {

  productId: string;

  productName: string;

  cover?: string;

  minPrice?: number | string;

}



const CARD_START = '<<<PRODUCT_CONSULT>>>';

const CARD_END = '<<<END_CARD>>>';



export const AGENT_CONSULT_PRODUCT_KEY = 'eshop_agent_consult_product';





export function getAgentConsultStorageKey(userId?: string | null): string {

  const id = userId?.trim();

  return id ? `${AGENT_CONSULT_PRODUCT_KEY}:userId:${id}` : AGENT_CONSULT_PRODUCT_KEY;

}



export function buildProductConsultMessage(product: AgentConsultProduct): string {

  const payload = JSON.stringify({

    productId: product.productId,

    productName: product.productName,

    cover: product.cover || '',

    minPrice: product.minPrice != null && product.minPrice !== '' ? Number(product.minPrice) : null

  });

  const line = `【商品咨询】我想了解「${product.productName}」（商品编号：${product.productId}）`;

  return `${CARD_START}${payload}${CARD_END}\n${line}`;

}



export function parseProductConsultMessage(userMessage?: string | null): {

  card: AgentConsultProduct | null;

  text: string;

} {

  if (!userMessage) return { card: null, text: '' };

  const start = userMessage.indexOf(CARD_START);

  if (start < 0) return { card: null, text: userMessage };

  const end = userMessage.indexOf(CARD_END, start + CARD_START.length);

  if (end < 0) return { card: null, text: userMessage };



  let card: AgentConsultProduct | null = null;

  try {

    const obj = JSON.parse(userMessage.slice(start + CARD_START.length, end));

    if (obj?.productId && obj?.productName) {

      card = {

        productId: String(obj.productId),

        productName: String(obj.productName),

        cover: obj.cover ? String(obj.cover) : undefined,

        minPrice: obj.minPrice

      };

    }

  } catch {

    

  }



  let text = userMessage.slice(end + CARD_END.length);

  if (text.startsWith('\n')) text = text.slice(1);

  return { card, text: text.trim() };

}



export function saveAgentConsultProduct(product: AgentConsultProduct, userId?: string | null) {

  sessionStorage.setItem(getAgentConsultStorageKey(userId), JSON.stringify(product));

}



export function loadAgentConsultProduct(userId?: string | null): AgentConsultProduct | null {

  const raw = sessionStorage.getItem(getAgentConsultStorageKey(userId));

  if (!raw) return null;

  try {

    const o = JSON.parse(raw);

    if (o?.productId && o?.productName) {

      return {

        productId: String(o.productId),

        productName: String(o.productName),

        cover: o.cover,

        minPrice: o.minPrice

      };

    }

  } catch {

    

  }

  return null;

}



export function clearAgentConsultProduct(userId?: string | null) {

  sessionStorage.removeItem(getAgentConsultStorageKey(userId));

}


