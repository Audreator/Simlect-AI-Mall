#!/usr/bin/env python3
"""升级通用

from __future__ import annotations

import re
from pathlib import Path

MARKER = "# [zh]"
GENERIC = ("可执行语句", "赋值语句", "赋值 `model_config`", "赋值 `default`", "赋值 `validation_alias`")

def _hint_for_code(code: str) -> str | None:
    code = code.strip()
    # 尝试加载精确映射
    try:
        from enhance_zh_comments import LINE_HINTS
        if code in LINE_HINTS:
            return LINE_HINTS[code]
    except ImportError:
        pass
    # 启发式
    if code.startswith(("from ", "import ")):
        return None  # 已有较好注释
    if m := re.match(r'^"([^"]+)":\s*(.+?),?\s*$', code):
        k, v = m.group(1), m.group(2)
        return f"Map 键 `{k}` -> {v}（类比 Java Map.put）"
    m = re.match(r"^(\w+):\s*(.+)$", code)
    if m and not code.endswith(":"):
        name, typ = m.group(1), m.group(2)
        if "dict" in typ:
            return f"字段 `{name}` 类型 Map（类比 Java Map）"
        if "list" in typ:
            return f"字段 `{name}` 类型 List（类比 Java List）"
        if "str" in typ:
            return f"字段 `{name}` 字符串字段"
        if "int" in typ:
            return f"字段 `{name}` 整型字段"
        if "bool" in typ:
            return f"字段 `{name}` 布尔字段"
    if code.startswith("await "):
        inner = code[6:]
        return f"await 等待异步完成：{inner[:60]}（类比 CompletableFuture.get）"
    if code.startswith("return "):
        val = code[7:].strip().rstrip(",")
        if val in ("None", "False", "True"):
            return f"返回 {val}（类比 Java return {val if val != 'None' else 'null'}）"
        if val.startswith("{") or val.startswith("["):
            return f"返回集合/Map 字面量（类比 Java return new HashMap<>()）"
        if val.startswith('"') or val.startswith("'"):
            return f"返回字符串常量"
        if val.startswith("f"):
            return f"返回 f-string 格式化字符串（类比 String.format）"
        return f"返回表达式 `{val[:50]}`"
    if code.startswith("logger."):
        return f"结构化日志：{code.split('(')[0]}（类比 SLF4J logger）"
    if ".append(" in code or ".extend(" in code:
        return f"向 List 追加元素（类比 list.add / addAll）"
    if ".get(" in code or '["' in code or "['" in code:
        return f"从 Map 读取键值（类比 map.get(key)）"
    if code.endswith("= None") or code.endswith("= None,"):
        name = code.split("=")[0].strip()
        return f"初始化 `{name}` 为 null（类比 Optional.empty）"
    if code.endswith("= []") or code.endswith("= [],"):
        name = code.split("=")[0].strip()
        return f"初始化 `{name}` 为空 List（类比 new ArrayList<>()）"
    if code.endswith("= {}") or code.endswith("= {},"):
        name = code.split("=")[0].strip()
        return f"初始化 `{name}` 为空 Map（类比 new HashMap<>()）"
    if code.endswith("= False") or code.endswith("= True"):
        name = code.split("=")[0].strip()
        val = "true" if "True" in code else "false"
        return f"设置 `{name}` = {val}"
    if code.endswith("= 0") or code.endswith("= 0,"):
        name = code.split("=")[0].strip()
        return f"计数器 `{name}` 初始化为 0"
    if m := re.match(r"^(\w+)\s*=\s*(.+)$", code):
        name, val = m.group(1), m.group(2)
        if name.isupper():
            return f"模块常量 `{name}` = {val[:40]}"
        return f"局部变量 `{name}` 赋值"
    if code in ("}", ")", "])", "}),"):
        return "闭合括号/集合字面量"
    if code.endswith(":"):
        return None
    if code.startswith(("if ", "elif ", "else:", "for ", "while ", "try:", "except ", "finally:", "with ", "raise ", "yield ", "pass", "break", "continue", "assert ")):
        return None  # 控制流已有注释
    if code.startswith("@") or code.startswith("async def") or code.startswith("def ") or code.startswith("class "):
        return None
    return f"执行：{code[:55]}{'...' if len(code) > 55 else ''}"

def enhance_file(path: Path) -> int:
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    n = 0
    out: list[str] = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.strip().startswith(MARKER) and i + 1 < len(lines):
            body = line.strip()[len(MARKER) :].strip()
            nxt = lines[i + 1].strip()
            if any(g in body for g in GENERIC):
                new_hint = _hint_for_code(nxt)
                if new_hint and new_hint != body:
                    indent = re.match(r"^(\s*)", line).group(1)
                    out.append(f"{indent}{MARKER} {new_hint}\n")
                    n += 1
                    i += 1
                    continue
        out.append(line)
        i += 1
    if n:
        path.write_text("".join(out), encoding="utf-8")
    return n

if __name__ == "__main__":
    import sys
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    dirs = [
        Path(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\graph"),
        Path(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\domain"),
        Path(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\api"),
        Path(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\utils"),
        Path(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\memory"),
        Path(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\harness"),
        Path(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\mcp"),
        Path(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\rag"),
        Path(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\config"),
        Path(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\models"),
        Path(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\resilience"),
        Path(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\mcp_server"),
    ]
    files = []
    for d in dirs:
        files.extend(d.rglob("*.py"))
    files.append(Path(r"d:\project\EShop\Simlect\Simlect-backend\Simlect-agent\app\constants.py"))
    total = sum(enhance_file(p) for p in sorted(set(files)))
    print(f"upgraded {total} comment lines")
