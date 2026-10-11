"""限流 key 解析测试：有 token 用 token；无 token 回退客户端 IP（修复 NameError 隐患）。"""

from starlette.requests import Request

from app.api.routes.agent import _user_key


def _make_request(headers=None):
    return Request(
        {
            "type": "http",
            "headers": headers or [],
            "client": ("1.2.3.4", 1234),
            "method": "POST",
            "path": "/api/agent/sendMessage",
            "query_string": b"",
        }
    )


def test_user_key_prefers_token_header():
    assert _user_key(_make_request([(b"token", b"tk-1")])) == "tk-1"


def test_user_key_falls_back_to_client_ip():
    assert _user_key(_make_request()) == "1.2.3.4"