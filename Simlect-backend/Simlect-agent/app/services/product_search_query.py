"""商品搜索查询模块：规范化口语化搜索词、扩展主题同义词，并过滤混合检索中的无关商品。"""

from __future__ import annotations  # import statement — 延迟类型注解

import re  # import statement — 正则表达式，类似 Java Pattern

# Longest-first topic hints extracted from user chatter.
_TOPIC_HINTS = (  # 主题提示词元组 — 按长度优先匹配
    "台式机",
    "笔记本",
    "尤克里里",
    "电子琴",
    "iphone",
    "零食",
    "小吃",
    "坚果",
    "糖果",
    "饼干",
    "手机",
    "苹果",
    "三星",
    "华为",
    "小米",
    "oppo",
    "vivo",
    "荣耀",
    "电脑",
    "台式",
    "平板",
    "主机",
    "显示器",
    "玩具",
    "玩偶",
    "模型",
    "积木",
    "乐高",
    "吉他",
    "乐器",
    "钢琴",
    "家电",
    "服饰",
    "衣服",
    "鞋子",
    "美妆",
    "护肤",
    "儿童",
)

# When user asks for a topic, accept related tokens in product titles.
_TOPIC_EXPAND: dict[str, tuple[str, ...]] = {  # dict 类似 Map<String, List<String>> — 主题扩展同义词
    "零食": (
        "零食",
        "小吃",
        "坚果",
        "糖果",
        "饼干",
        "薯片",
        "雪饼",
        "辣条",
        "巧克力",
        "膨化",
        "果干",
        "肉脯",
        "糕点",
        "锅巴",
        "虾条",
        "牛肉干",
        "瓜子",
        "旺旺",
        "奥利奥",
    ),
    "小吃": ("小吃", "零食", "糕点", "小吃货"),
    "手机": (
        "手机",
        "iphone",
        "苹果",
        "华为",
        "小米",
        "三星",
        "oppo",
        "vivo",
        "荣耀",
        "红米",
        "手机壳",
    ),
    "电脑": ("电脑", "台式", "笔记本", "主机", "显示器", "一体机"),
    "玩具": ("玩具", "玩偶", "公仔", "积木", "乐高", "模型", "毛绒"),
    "吉他": ("吉他", "尤克里里", "乐器", "民谣", "电吉他"),
}

_FILLERS = re.compile(  # 口语填充词正则 — 如「我要」「帮我」
    r"(我想要|我要|想要|想买|帮我|给我|麻烦|请你|请|"
    r"有没有|能不能|可以吗|可以|推荐一下|推荐|"
    r"看看|买点|来点|吃点|搜一下|搜索一下|搜索|"
    r"找找|找|买|要|吃)"
)
_PUNCT = re.compile(r"[的了吗呢啊哦呀呗嘛～~，。！？、；：""''\s]+")  # 标点与语气词

def normalize_product_search_query(text: str | None) -> str:  # 规范化搜索词 — 「我要吃零食」→「零食」
    """Turn「我要吃零食」into「零食」; keep concrete keywords otherwise."""
    t = (text or "").strip()  # null 安全去空白
    if not t:
        return ""
    lower = t.lower()  # 转小写便于匹配
    for hint in sorted(_TOPIC_HINTS, key=len, reverse=True):  # 最长优先匹配主题
        if hint.lower() in lower:
            return hint  # 命中主题词直接返回
    cleaned = _FILLERS.sub("", t)  # 去除填充词
    cleaned = _PUNCT.sub("", cleaned).strip()  # 去除标点
    return cleaned or t  # 清洗后为空则保留原文

def match_terms_for_query(query: str | None) -> list[str]:  # 生成匹配词列表 — 含扩展同义词
    q = (query or "").strip()
    if not q:
        return []
    terms: list[str] = []  # List<String> 输出
    seen: set[str] = set()  # set 类似 HashSet — 去重

    def _add(term: str) -> None:  # 内部方法 — 添加不重复 term
        t = (term or "").strip().lower()
        if len(t) < 2 or t in seen:  # 过短或已存在则跳过
            return
        seen.add(t)
        terms.append(t)

    topic = normalize_product_search_query(q)  # 规范化主题
    _add(topic)
    _add(q)
    for key, expand in _TOPIC_EXPAND.items():  # 遍历主题扩展表
        if key in q or key in topic or key.lower() in q.lower():  # 命中主题
            for e in expand:
                _add(e)  # 添加同义词
    return terms

def product_matches_query_terms(product: dict, terms: list[str]) -> bool:  # 商品标题/描述是否含任一匹配词
    if not terms:
        return True  # 无 terms 则全部匹配
    name = str(product.get("product_name") or product.get("productName") or "").lower()
    desc = str(
        product.get("product_desc")
        or product.get("productDesc")
        or product.get("description")
        or ""
    ).lower()
    hay = f"{name} {desc}"  # 拼接检索 haystack
    return any(term in hay for term in terms)  # any 类似 Stream.anyMatch

def filter_products_by_query_relevance(products: list[dict], query: str | None) -> list[dict]:  # 过滤混合检索无关商品
    """Drop hybrid hits that share no topic tokens with the user query."""
    if not products:
        return []
    terms = match_terms_for_query(query)
    if not terms:
        return list(products)  # 无 terms 则原样返回
    return [p for p in products if product_matches_query_terms(p, terms)]  # list 推导 — 类似 Stream.filter
