from app.utils.ws_token import resolve_ws_token


def test_resolve_ws_token_query_ignored():
    # query token 已废弃（防日志/Referer 泄露 + 无法 Origin 绑定）：一律忽略
    assert resolve_ws_token("abc123") is None


def test_resolve_ws_token_from_cookie():
    assert resolve_ws_token(None, cookie_token="cookie_token") == "cookie_token"


def test_resolve_ws_token_cookie_over_query():
    # cookie 优先级高于（废弃的）query：query 不参与选择
    assert resolve_ws_token("query_token", cookie_token="cookie_token") == "cookie_token"


def test_resolve_ws_token_header():
    assert resolve_ws_token(None, header_token="header_token") == "header_token"
