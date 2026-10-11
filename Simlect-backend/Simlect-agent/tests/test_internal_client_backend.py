"""内部读客户端后端选择测试：默认 http；可切换 db（单体直连库适配）。"""

from app.config.settings import Settings
from app.services.java_internal_client import JavaInternalClient, build_internal_client


def test_default_internal_read_backend_is_http():
    assert Settings.model_fields["internal_read_backend"].default == "http"


def test_build_internal_client_http_backend():
    client = build_internal_client("http")
    assert isinstance(client, JavaInternalClient)


def test_build_internal_client_db_backend():
    client = build_internal_client("db")
    assert type(client).__name__ == "JavaInternalClientDb"
    assert hasattr(client, "list_orders")
    assert hasattr(client, "search_on_sale")