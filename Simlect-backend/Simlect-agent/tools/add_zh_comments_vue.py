#!/usr/bin/env python3
"""为 Vue/TS 源码追加详细中文注释。"""

from __future__ import annotations

import re
import sys
from pathlib import Path

MARKER_TS = "// [zh]"
MARKER_HTML = "<!-- [zh]"

def _ts_comment(line: str) -> str | None:
    s = line.strip()
    if not s or MARKER_TS in line or s.startswith("//") or s.startswith("*"):
        return None
    if s.startswith("import "):
        m = re.match(r"import\s+(?:\{[^}]+\}|\*\s+as\s+\w+|\w+)\s+from\s+['\"]([^'\"]+)['\"]", s)
        mod = m.group(1) if m else "模块"
        return f"导入 `{mod}`"
    if s.startswith("export "):
        return "导出模块成员"
    if re.match(r"^(const|let|var)\s+(\w+)", s):
        return f"声明变量 `{re.match(r'^(?:const|let|var)\s+(\w+)', s).group(1)}`"
    if s.startswith("function ") or re.match(r"^(async\s+)?function\s+\w+", s):
        m = re.search(r"function\s+(\w+)", s)
        return f"函数 `{m.group(1) if m else 'anonymous'}`"
    if re.match(r"^async\s+function\s+(\w+)", s):
        return f"异步函数 `{re.match(r'^async\\s+function\\s+(\\w+)', s).group(1)}`"
    if s.startswith("interface ") or s.startswith("type "):
        m = re.search(r"(?:interface|type)\s+(\w+)", s)
        return f"TypeScript 类型 `{m.group(1) if m else ''}`（类比 Java interface/DTO）"
    if s.startswith("if ") or s.startswith("if("):
        return "条件判断 if"
    if s.startswith("else"):
        return "else 分支"
    if s.startswith("for ") or s.startswith("for("):
        return "for 循环"
    if s.startswith("return "):
        return "return 返回"
    if s.startswith("await "):
        return "await 等待 Promise（类比 CompletableFuture.get）"
    if s.endswith("{") and not s.startswith("@"):
        return "代码块开始"
    if s.endswith(";") or s.startswith("@"):
        if s.startswith("@"):
            return f"装饰器/注解 `{s.split('(')[0]}`"
        return "语句"
    return None

def _html_comment(line: str) -> str | None:
    s = line.strip()
    if not s or MARKER_HTML in line or s.startswith("<!--") or s.startswith("</"):
        return None
    if s.startswith("<template") or s.startswith("<script") or s.startswith("<style"):
        return "SFC 区块开始"
    if re.match(r"^<[A-Za-z]", s):
        tag = re.match(r"^<([A-Za-z][\w-]*)", s)
        name = tag.group(1) if tag else "element"
        return f"模板元素 `<{name}>`"
    if s.startswith("{{"):
        return "Mustache 插值表达式"
    return None

def annotate_vue(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    if MARKER_TS in text or MARKER_HTML in text:
        return False
    lines = text.splitlines(keepends=True)
    out: list[str] = []
    section = "unknown"
    changed = False
    for line in lines:
        s = line.strip().lower()
        if s.startswith("<script"):
            section = "script"
        elif s.startswith("<template"):
            section = "template"
        elif s.startswith("<style"):
            section = "style"
        elif s.startswith("</script") or s.startswith("</template") or s.startswith("</style"):
            section = "unknown"
        comment = None
        if section == "script":
            comment = _ts_comment(line)
            marker = MARKER_TS
        elif section == "template":
            comment = _html_comment(line)
            marker = MARKER_HTML
        else:
            comment = None
            marker = ""
        if comment:
            indent = re.match(r"^(\s*)", line).group(1)
            if section == "template":
                out.append(f"{indent}{marker} {comment} -->\n")
            else:
                out.append(f"{indent}{marker} {comment}\n")
            changed = True
        out.append(line)
    if not changed:
        return False
    path.write_text("".join(out), encoding="utf-8")
    return True

def annotate_ts(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    if MARKER_TS in text:
        return False
    out: list[str] = []
    changed = False
    for line in text.splitlines(keepends=True):
        comment = _ts_comment(line)
        if comment:
            indent = re.match(r"^(\s*)", line).group(1)
            out.append(f"{indent}{MARKER_TS} {comment}\n")
            changed = True
        out.append(line)
    if not changed:
        return False
    path.write_text("".join(out), encoding="utf-8")
    return True

def collect_files(roots: list[Path]) -> list[Path]:
    files: list[Path] = []
    for root in roots:
        if root.is_file():
            files.append(root)
        else:
            for p in root.rglob("*"):
                if "node_modules" in str(p):
                    continue
                if p.suffix in (".vue", ".ts", ".tsx"):
                    files.append(p)
    return sorted({x.resolve() for x in files})

def main(argv: list[str]) -> int:
    args = argv[1:]
    if not args:
        print("Usage: add_zh_comments_vue.py <file-or-dir> ...")
        return 1
    files = collect_files([Path(a) for a in args])
    changed = 0
    for p in files:
        ok = annotate_vue(p) if p.suffix == ".vue" else annotate_ts(p)
        if ok:
            changed += 1
            print(p)
    print(f"DONE {changed}/{len(files)}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
