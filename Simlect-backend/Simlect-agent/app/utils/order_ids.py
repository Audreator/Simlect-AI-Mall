"""Order / order-item id extraction.

Java generates orderItemId as ``{orderId}_{n}`` (e.g. ``...QK_1``).
A bare order-id regex must not swallow the ``_n`` suffix when the user typed an item id.
"""

from __future__ import annotations

import re

# orderId: long digit prefix + alphanumeric tail (no underscore)
_ORDER_ID_RE = re.compile(r"\d{10,}[A-Za-z0-9]{8,}")
# orderItemId: orderId + _ + item index
_ORDER_ITEM_ID_RE = re.compile(r"(\d{10,}[A-Za-z0-9]{8,}_\d+)")

def extract_order_item_id(*texts: str | None) -> str | None:
    for text in texts:
        if not text:
            continue
        m = _ORDER_ITEM_ID_RE.search(str(text))
        if m:
            return m.group(1)
    return None

def extract_order_id(*texts: str | None) -> str | None:
    """Return orderId. If text has orderItemId ``xxx_1``, return ``xxx``."""
    item = extract_order_item_id(*texts)
    if item:
        return item.rsplit("_", 1)[0]
    for text in texts:
        if not text:
            continue
        m = _ORDER_ID_RE.search(str(text))
        if m:
            return m.group(0)
    return None

def extract_refund_target_id(*texts: str | None) -> str | None:
    """Prefer orderItemId when present; otherwise orderId."""
    return extract_order_item_id(*texts) or extract_order_id(*texts)
