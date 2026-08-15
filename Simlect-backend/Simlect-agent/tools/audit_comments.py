#!/usr/bin/env python3
"""统计尚未在上一行配备中文注释的代码行。"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
SKIP = {"node_modules", ".venv", "target", "dist", "site-packages", "__pycache__"}

def is_comment_line(s: str, lang: str) -> bool:
    s = s.strip()
    if not s:
        return True
    if lang == "py" and (s.startswith("#") or PY_OK(s)):
        return True
    if lang == "java" and (s.startswith("//") or s.startswith("*") or s.startswith("/*") or s.startswith("*/")):
        return True
    if lang in ("vue", "ts") and (s.startswith("//") or s.startswith("<!--") or s.startswith("/*") or "[zh]" in s):
        return True
    return False

def PY_OK(s):
    return s.startswith('"""') or s.startswith("'''")

def audit_file(path: Path) -> int:
    text = path.read_text(encoding="utf-8", errors="ignore")
    lines = text.splitlines()
    if path.suffix == ".java":
        lang = "java"
    elif path.suffix == ".py":
        lang = "py"
    elif path.suffix == ".vue":
        lang = "vue"
    else:
        lang = "ts"
    missing = 0
    prev = ""
    in_block = False
    for line in lines:
        s = line.strip()
        if lang == "java":
            if "/*" in s and "*/" not in s:
                in_block = True
            if in_block:
                if "*/" in s:
                    in_block = False
                prev = line
                continue
        if not s or s in ("{", "}", "};", ")", "(", "],", "],"):
            prev = line
            continue
        if is_comment_line(s, lang):
            prev = line
            continue
        if "[zh]" in prev or (lang == "py" and prev.strip().startswith("#")):
            prev = line
            continue
        if lang == "py" and "#" in line and ("[zh]" in line or (not line.strip().startswith("#") and "#" in line)):
            prev = line
            continue
        missing += 1
        prev = line
    return missing

def main():
    totals = {"py": [0, 0], "java": [0, 0], "vue": [0, 0], "ts": [0, 0]}
    samples = []
    roots = [
        (ROOT / "Simlect/Simlect-backend/Simlect-agent/app", "*.py", "py"),
        (ROOT / "Simlect/Simlect-backend/Simlect-agent/tests", "*.py", "py"),
        (ROOT / "ragent-python/app", "*.py", "py"),
        (ROOT / "ragent-python/mcp_server", "*.py", "py"),
        (ROOT / "ragent-python/tests", "*.py", "py"),
        (ROOT / "Simlect/Simlect-backend", "*.java", "java"),
        (ROOT / "ragent", "*.java", "java"),
        (ROOT / "Simlect/Simlect-front/Simlect-web/src", "*.vue", "vue"),
        (ROOT / "Simlect/Simlect-front/Simlect-admin/src", "*.vue", "vue"),
        (ROOT / "Simlect/Simlect-front/Simlect-web/src", "*.ts", "ts"),
        (ROOT / "Simlect/Simlect-front/Simlect-admin/src", "*.ts", "ts"),
        (ROOT / "ragent/frontend/src", "*.tsx", "ts"),
        (ROOT / "ragent/frontend/src", "*.ts", "ts"),
    ]
    for root, glob, kind in roots:
        if not root.exists():
            continue
        for p in root.rglob(glob):
            if any(x in p.parts for x in SKIP):
                continue
            if kind == "java" and "src/main/java" not in str(p).replace("\\", "/"):
                continue
            m = audit_file(p)
            totals[kind][0] += 1
            totals[kind][1] += m
            if m > 50 and len(samples) < 5:
                samples.append((str(p), m))
    for k, (files, miss) in totals.items():
        print(f"{k}: {files} files, {miss} uncommented code lines")
    for p, m in samples:
        print(f"  sample {m} lines: {p}")

if __name__ == "__main__":
    main()
