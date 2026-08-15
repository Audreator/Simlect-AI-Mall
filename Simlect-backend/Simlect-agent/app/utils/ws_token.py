def resolve_ws_token(
    query_token: str | None,
    cookie_token: str | None = None,
    header_token: str | None = None,
):
    # 仅信任 Cookie / Header 来源的 token：
    # query token 会进入日志/Referer 造成泄露，且无法做 Origin 绑定 —— 已废弃（调用处一律传 None）
    if cookie_token and cookie_token.strip():
        return cookie_token.strip()
    if header_token and header_token.strip():
        return header_token.strip()
    return None
