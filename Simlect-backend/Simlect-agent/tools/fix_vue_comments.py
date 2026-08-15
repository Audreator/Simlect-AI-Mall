#!/usr/bin/env python3
"""修复 Vue 模板中误插入在标签属性之间的 HTML 注释行。"""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
SKIP = {"node_modules", ".venv", "target", "dist"}

def fix_vue(content: str) -> str:
    lines = content.splitlines(keepends=True)
    out: list[str] = []
    section = "unknown"
    in_open_tag = False
    for line in lines:
        s = line.strip()
        low = s.lower()
        if low.startswith("<script"):
            section = "script"
            in_open_tag = False
        elif low.startswith("<template"):
            section = "template"
            in_open_tag = False
        elif low.startswith("<style"):
            section = "style"
            in_open_tag = False
        elif low.startswith("</script") or low.startswith("</template") or low.startswith("</style"):
            section = "unknown"
            in_open_tag = False
        if section == "template":
            if s.startswith("<!-- [zh]") and in_open_tag:
                continue  # 删除标签内部的非法注释
            if s.startswith("<") and not s.startswith("</") and not s.endswith("/>") and ">" not in s[1:]:
                in_open_tag = True
            elif in_open_tag and ">" in s:
                in_open_tag = False
            elif s.startswith("<") and s.endswith(">"):
                in_open_tag = False
        out.append(line)
    return "".join(out)

def main() -> None:
    n = 0
    for p in ROOT.rglob("*.vue"):
        if any(x in p.parts for x in SKIP):
            continue
        text = p.read_text(encoding="utf-8")
        fixed = fix_vue(text)
        if fixed != text:
            p.write_text(fixed, encoding="utf-8")
            n += 1
    print(f"fixed {n} vue files")

if __name__ == "__main__":
    main()
