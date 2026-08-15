#!/usr/bin/env python3
"""
确保 Java / Python / Vue·TS·TSX 每一行有意义代码都有详细中文注释。
对已注释行不重复添加；可多次运行（幂等）。
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
SKIP_PARTS = {"node_modules", ".venv", "target", "dist", "site-packages", "__pycache__", "build"}

PY_MARKER = "# [zh]"
JAVA_MARKER = "// [zh]"
TS_MARKER = "// [zh]"
HTML_MARKER = "<!-- [zh]"

# 复用现有逻辑
sys.path.insert(0, str(Path(__file__).parent))
from add_zh_comments import _comment_for_line as py_comment, _docstring_state  # noqa: E402
from add_zh_comments_java import _comment_for_line as java_comment, _block_state  # noqa: E402
from add_zh_comments_vue import _ts_comment  # noqa: E402

def _html_comment(line: str) -> str | None:
    s = line.strip()
    if not s:
        return None
    if s.startswith("<template") or s.startswith("<script") or s.startswith("<style"):
        return "SFC 区块开始（单文件组件 template/script/style 三段）"
    if s.startswith("</"):
        tag = re.match(r"</(\w[\w-]*)", s)
        name = tag.group(1) if tag else "element"
        return f"闭合标签 `</{name}>`"
    if re.match(r"^<[A-Za-z]", s):
        tag = re.match(r"^<([A-Za-z][\w-]*)", s)
        name = tag.group(1) if tag else "element"
        return f"开始标签 `<{name}>`"
    if s.startswith("{{"):
        return "Mustache 插值表达式"
    return f"模板内容：`{s[:45]}`"

def _ts_comment_enhanced(line: str) -> str | None:
    c = _ts_comment(line)
    if c:
        return c
    s = line.strip()
    if not s:
        return None
    if s in ("}", "{", ")", "(", "],", "];", ">", " />"):
        return "语法符号"
    return f"TypeScript/JS 语句：`{s[:55]}`"

def _skip_path(p: Path) -> bool:
    return any(s in p.parts for s in SKIP_PARTS)

def _prev_is_comment(prev: str | None, marker: str) -> bool:
    if not prev:
        return False
    s = prev.strip()
    if marker in prev or "[zh]" in prev:
        return True
    if s.startswith("//") or s.startswith("#") or s.startswith("*") or s.startswith("<!--"):
        return True
    if s.startswith("/*") or s.startswith("*/"):
        return True
    return False

def _style_comment(line: str) -> str | None:
    s = line.strip()
    if not s or s.startswith("/*") or s.startswith("*") or s.startswith("//"):
        return None
    if s.startswith("@") or re.match(r"^[\w-]+\s*:", s) or s.endswith("{") or s.endswith("}"):
        return f"样式规则 `{s[:40]}`"
    return "样式声明"

def annotate_python(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines(keepends=True)
    out: list[str] = []
    doc_state: str | None = None
    changed = False
    prev: str = ""
    for line in lines:
        doc_state = _docstring_state(line, doc_state)
        in_doc = doc_state is not None
        stripped = line.strip()
        if (
            stripped
            and not in_doc
            and not stripped.startswith("#")
            and "[zh]" not in line
            and not _prev_is_comment(prev, PY_MARKER)
        ):
            c = py_comment(line, in_doc)
            if c:
                indent = re.match(r"^(\s*)", line).group(1)
                out.append(f"{indent}{PY_MARKER} {c}\n")
                changed = True
        out.append(line)
        prev = out[-1]
    if changed:
        path.write_text("".join(out), encoding="utf-8")
    return changed

def annotate_java(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines(keepends=True)
    out: list[str] = []
    in_block = False
    in_javadoc = False
    changed = False
    prev: str = ""
    for line in lines:
        in_block, in_javadoc = _block_state(line, in_block, in_javadoc)
        stripped = line.strip()
        if (
            stripped
            and not in_javadoc
            and not in_block
            and JAVA_MARKER not in line
            and not (stripped.startswith("//") and JAVA_MARKER not in line)
            and not _prev_is_comment(prev, JAVA_MARKER)
        ):
            c = java_comment(line, in_block, in_javadoc)
            if c:
                indent = re.match(r"^(\s*)", line).group(1)
                out.append(f"{indent}{JAVA_MARKER} {c}\n")
                changed = True
        out.append(line)
        prev = out[-1]
    if changed:
        path.write_text("".join(out), encoding="utf-8")
    return changed

def _line_is_comment_only(stripped: str, section: str) -> bool:
    if not stripped:
        return True
    if "[zh]" in stripped:
        return True
    if section == "script" and stripped.startswith("//"):
        return True
    if section == "template" and (stripped.startswith("<!--") or stripped.endswith("-->")):
        return True
    if section == "style" and (stripped.startswith("/*") or stripped.startswith("*") or stripped.endswith("*/")):
        return True
    return False

def annotate_vue(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines(keepends=True)
    out: list[str] = []
    section = "unknown"
    changed = False
    prev: str = ""
    in_open_tag = False
    for line in lines:
        s_lower = line.strip().lower()
        if s_lower.startswith("<script"):
            section = "script"
            in_open_tag = False
        elif s_lower.startswith("<template"):
            section = "template"
            in_open_tag = False
        elif s_lower.startswith("<style"):
            section = "style"
            in_open_tag = False
        elif s_lower.startswith("</script") or s_lower.startswith("</template") or s_lower.startswith("</style"):
            section = "unknown"
            in_open_tag = False
        stripped = line.strip()
        skip_template = section == "template" and in_open_tag
        if section == "template" and stripped:
            if stripped.startswith("<") and not stripped.startswith("</") and not stripped.endswith("/>") and ">" not in stripped[1:]:
                in_open_tag = True
            elif in_open_tag and ">" in stripped:
                in_open_tag = False
            elif stripped.startswith("<") and stripped.endswith(">"):
                in_open_tag = False
        if (
            section in ("script", "template", "style")
            and stripped
            and not skip_template
            and not _line_is_comment_only(stripped, section)
        ):
            if not _prev_is_comment(prev, TS_MARKER) and not (section == "template" and HTML_MARKER in prev):
                comment = None
                if section == "script":
                    comment = _ts_comment_enhanced(line)
                elif section == "template":
                    comment = _html_comment(line)
                else:
                    comment = _style_comment(line)
                if comment:
                    indent = re.match(r"^(\s*)", line).group(1)
                    if section == "template":
                        out.append(f"{indent}{HTML_MARKER} {comment} -->\n")
                    elif section == "style":
                        out.append(f"{indent}/* [zh] {comment} */\n")
                    else:
                        out.append(f"{indent}{TS_MARKER} {comment}\n")
                    changed = True
        out.append(line)
        prev = out[-1]
    if changed:
        path.write_text("".join(out), encoding="utf-8")
    return changed

def annotate_ts(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines(keepends=True)
    out: list[str] = []
    changed = False
    prev: str = ""
    for line in lines:
        stripped = line.strip()
        if stripped and not _line_is_comment_only(stripped, "script") and not _prev_is_comment(prev, TS_MARKER):
            c = _ts_comment_enhanced(line)
            if c:
                indent = re.match(r"^(\s*)", line).group(1)
                out.append(f"{indent}{TS_MARKER} {c}\n")
                changed = True
        out.append(line)
        prev = out[-1]
    if changed:
        path.write_text("".join(out), encoding="utf-8")
    return changed

def collect_all() -> dict[str, list[Path]]:
    groups: dict[str, list[Path]] = {"py": [], "java": [], "vue": [], "ts": []}
    py_roots = [
        ROOT / "Simlect" / "Simlect-backend" / "Simlect-agent" / "app",
        ROOT / "Simlect" / "Simlect-backend" / "Simlect-agent" / "tests",
        ROOT / "ragent-python" / "app",
        ROOT / "ragent-python" / "mcp_server",
        ROOT / "ragent-python" / "tests",
    ]
    for r in py_roots:
        if r.exists():
            groups["py"].extend(p for p in r.rglob("*.py") if not _skip_path(p))
    java_roots = [ROOT / "Simlect" / "Simlect-backend", ROOT / "ragent"]
    for r in java_roots:
        if r.exists():
            groups["java"].extend(
                p for p in r.rglob("*.java")
                if "src/main/java" in str(p).replace("\\", "/") and not _skip_path(p)
            )
    fe_roots = [
        ROOT / "Simlect" / "Simlect-front" / "Simlect-web" / "src",
        ROOT / "Simlect" / "Simlect-front" / "Simlect-admin" / "src",
        ROOT / "ragent" / "frontend" / "src",
    ]
    for r in fe_roots:
        if r.exists():
            for p in r.rglob("*"):
                if _skip_path(p) or not p.is_file():
                    continue
                if p.suffix == ".vue":
                    groups["vue"].append(p)
                elif p.suffix in (".ts", ".tsx"):
                    groups["ts"].append(p)
    for k in groups:
        groups[k] = sorted({x.resolve() for x in groups[k]})
    return groups

def audit(groups: dict[str, list[Path]]) -> dict[str, tuple[int, int]]:
    stats = {}
    for kind, files in groups.items():
        missing = 0
        for p in files:
            text = p.read_text(encoding="utf-8", errors="ignore")
            lines = [ln for ln in text.splitlines() if ln.strip()]
            if not lines:
                continue
            if kind == "py":
                commented = sum(1 for ln in lines if PY_MARKER in ln or ln.strip().startswith("#"))
            elif kind == "java":
                commented = sum(1 for ln in lines if JAVA_MARKER in ln or ln.strip().startswith("//") or ln.strip().startswith("*"))
            else:
                commented = sum(1 for ln in lines if "[zh]" in ln or ln.strip().startswith("//") or ln.strip().startswith("<!--"))
            ratio = commented / len(lines)
            if ratio < 0.85:
                missing += 1
        stats[kind] = (len(files), missing)
    return stats

def main() -> None:
    groups = collect_all()
    print("=== 处理前 ===")
    for k, (total, miss) in audit(groups).items():
        print(f"  {k}: {total} files, low-coverage={miss}")
    changed = {"py": 0, "java": 0, "vue": 0, "ts": 0}
    for p in groups["py"]:
        if annotate_python(p):
            changed["py"] += 1
    for p in groups["java"]:
        if annotate_java(p):
            changed["java"] += 1
    for p in groups["vue"]:
        if annotate_vue(p):
            changed["vue"] += 1
    for p in groups["ts"]:
        if annotate_ts(p):
            changed["ts"] += 1
    print("=== 本次修改 ===")
    for k, n in changed.items():
        print(f"  {k}: {n} files updated")
    print("=== 处理后 ===")
    for k, (total, miss) in audit(groups).items():
        print(f"  {k}: {total} files, low-coverage={miss}")

if __name__ == "__main__":
    main()
