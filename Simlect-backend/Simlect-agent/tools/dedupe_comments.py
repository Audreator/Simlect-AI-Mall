#!/usr/bin/env python3
"""去除重复的中文注释行（保留紧邻代码前的一条）。"""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
SKIP = {"node_modules", ".venv", "target", "dist", "site-packages", "__pycache__", "build"}

def is_zh_comment(line: str) -> bool:
    s = line.strip()
    return "[zh]" in line and (
        s.startswith("#") or s.startswith("//") or s.startswith("<!--") or s.startswith("/*")
    )

def dedupe_lines(lines: list[str]) -> list[str]:
    out: list[str] = []
    for line in lines:
        if is_zh_comment(line) and out and is_zh_comment(out[-1]):
            out[-1] = line  # 保留最新一条
            continue
        out.append(line)
    return out

def process_file(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    new_lines = dedupe_lines(text.splitlines(keepends=True))
    new_text = "".join(new_lines)
    if new_text != text:
        path.write_text(new_text, encoding="utf-8")
        return True
    return False

def main() -> None:
    changed = 0
    patterns = ["*.py", "*.java", "*.vue", "*.ts", "*.tsx"]
    for pat in patterns:
        for p in ROOT.rglob(pat):
            if any(s in p.parts for s in SKIP):
                continue
            if p.suffix == ".java" and "src/main/java" not in str(p).replace("\\", "/"):
                continue
            if process_file(p):
                changed += 1
    print(f"deduped {changed} files")

if __name__ == "__main__":
    main()
