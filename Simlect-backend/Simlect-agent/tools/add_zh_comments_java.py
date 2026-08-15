#!/usr/bin/env python3
"""为 Java 源码逐行追加详细中文注释，不修改逻辑。"""

from __future__ import annotations

import re
import sys
from pathlib import Path

MARKER = "// [zh]"

SPRING_ANNOTATIONS = {
    "@RestController": "REST 控制器，等价 @Controller + @ResponseBody",
    "@Controller": "Spring MVC 控制器",
    "@Service": "业务服务 Bean，由 Spring 容器管理",
    "@Component": "通用 Spring 组件 Bean",
    "@Configuration": "配置类，可声明 @Bean",
    "@Bean": "向 Spring 容器注册 Bean 工厂方法",
    "@Autowired": "按类型自动注入依赖",
    "@Resource": "按名称/类型注入（JSR-250）",
    "@Value": "注入配置项 ${...}",
    "@RequestMapping": "类/方法级 URL 映射",
    "@GetMapping": "HTTP GET 映射",
    "@PostMapping": "HTTP POST 映射",
    "@PutMapping": "HTTP PUT 映射",
    "@DeleteMapping": "HTTP DELETE 映射",
    "@RequestBody": "将 JSON 请求体绑定为 Java 对象",
    "@RequestParam": "绑定查询参数或表单字段",
    "@PathVariable": "绑定 URL 路径变量",
    "@Valid": "启用 Jakarta Validation 校验",
    "@Transactional": "声明式事务（类比 @Transactional rollbackFor）",
    "@Mapper": "MyBatis Mapper 接口",
    "@TableName": "MyBatis-Plus 表名映射",
    "@TableId": "MyBatis-Plus 主键字段",
    "@TableField": "MyBatis-Plus 列映射",
    "@Data": "Lombok：生成 getter/setter/equals/hashCode",
    "@Slf4j": "Lombok：生成 SLF4J Logger",
    "@Builder": "Lombok：建造者模式",
    "@AllArgsConstructor": "Lombok：全参构造器",
    "@NoArgsConstructor": "Lombok：无参构造器",
    "@RequiredArgsConstructor": "Lombok：final 字段构造器",
    "@SpringBootApplication": "Spring Boot 启动类组合注解",
    "@EnableScheduling": "启用定时任务",
    "@Async": "异步方法执行",
    "@Cacheable": "方法结果缓存",
    "@CacheEvict": "清除缓存",
}

def _import_comment(line: str) -> str:
    m = re.match(r"^\s*import\s+(?:static\s+)?([\w.]+)(?:\.\*)?;", line)
    if not m:
        return "导入依赖"
    fq = m.group(1)
    simple = fq.split(".")[-1]
    hints = {
        "List": "Java 列表接口 List<T>",
        "Map": "Java 映射接口 Map<K,V>",
        "Set": "Java 集合 Set<T>",
        "Optional": "可空容器 Optional<T>",
        "Objects": "Objects 工具类（equals/requireNonNull 等）",
        "StringUtils": "字符串工具（Apache Commons / Spring）",
        "CollectionUtils": "集合工具类",
        "ResponseVO": "统一 API 响应包装 Result<T>",
        "HttpServletRequest": "Servlet HTTP 请求对象",
        "HttpServletResponse": "Servlet HTTP 响应对象",
    }
    extra = hints.get(simple, f"导入 `{simple}`")
    return f"导入 {fq}：{extra}"

def _annotation_comment(line: str) -> str:
    ann = line.strip().split("(")[0]
    for key, val in SPRING_ANNOTATIONS.items():
        if ann.startswith(key):
            return f"注解 {key}：{val}"
    return f"注解 {ann}"

def _method_comment(line: str) -> str:
    m = re.match(
        r"^\s*(?:public|private|protected)\s+(?:static\s+)?(?:final\s+)?(?:[\w<>,\[\]\s.?]+)\s+(\w+)\s*\(",
        line,
    )
    if m:
        return f"方法 `{m.group(1)}` 声明"
    m2 = re.match(r"^\s*(?:public|private|protected)\s+(\w+)\s*\(", line)
    if m2:
        return f"构造器 `{m2.group(1)}`"
    return "成员方法或构造器"

def _field_comment(line: str) -> str:
    m = re.match(
        r"^\s*(?:private|protected|public)\s+(?:static\s+)?(?:final\s+)?(?:[\w<>,\[\]\s.?]+)\s+(\w+)\s*[;=]",
        line,
    )
    if m:
        return f"字段 `{m.group(1)}`"
    return "字段或成员变量"

def _control_comment(line: str) -> str:
    s = line.strip()
    if s.startswith("if ("):
        return "条件分支 if"
    if s.startswith("else if"):
        return "else-if 分支"
    if s == "else {" or s == "else":
        return "else 默认分支"
    if s.startswith("for ("):
        return "for 循环"
    if s.startswith("while ("):
        return "while 循环"
    if s.startswith("switch ("):
        return "switch 多分支"
    if s.startswith("case "):
        return "switch case 分支"
    if s == "default:":
        return "switch default 分支"
    if s.startswith("try {"):
        return "try 块"
    if s.startswith("catch ("):
        return "catch 捕获异常"
    if s == "finally {":
        return "finally 清理"
    if s.startswith("return "):
        return "return 返回"
    if s.startswith("throw "):
        return "throw 抛出异常"
    return "控制流语句"

def _comment_for_line(line: str, in_block: bool, in_javadoc: bool) -> str | None:
    stripped = line.strip()
    if not stripped or MARKER in line:
        return None
    if in_javadoc or in_block:
        return None
    if stripped.startswith("//") and not stripped.startswith(MARKER):
        return None
    if stripped.startswith("/*") or stripped.startswith("*") or stripped.startswith("*/"):
        return None
    if stripped.startswith("package "):
        pkg = stripped.replace("package ", "").rstrip(";")
        return f"声明包 `{pkg}`"
    if stripped.startswith("import "):
        return _import_comment(line)
    if stripped.startswith("@"):
        return _annotation_comment(line)
    if re.match(r"^\s*(public|private|protected)\s+(?:static\s+)?(?:final\s+)?(?:class|interface|enum|record)\s+", line):
        m = re.search(r"\b(class|interface|enum|record)\s+(\w+)", line)
        if m:
            return f"声明 {m.group(1)} `{m.group(2)}`"
        return "类型声明"
    if re.match(r"^\s*(public|private|protected)\s+", line) and "(" in line:
        return _method_comment(line)
    if re.match(r"^\s*(public|private|protected)\s+", line):
        return _field_comment(line)
    if re.match(r"^\s*(if |else|for |while |switch |case |default:|try |catch |return |throw )", stripped):
        return _control_comment(line)
    if stripped in ("{", "}", "};"):
        return None
    if stripped.endswith("{") and not stripped.startswith("@"):
        return "代码块开始"
    if stripped.endswith(";"):
        return "语句结束"
    return "可执行语句"

def _block_state(line: str, in_block: bool, in_javadoc: bool) -> tuple[bool, bool]:
    stripped = line.strip()
    if in_javadoc:
        if "*/" in stripped:
            return in_block, False
        return in_block, True
    if stripped.startswith("/**"):
        return in_block, True
    if in_block:
        if "*/" in stripped:
            return False, in_javadoc
        return True, in_javadoc
    if "/*" in stripped and "*/" not in stripped:
        return True, in_javadoc
    return in_block, in_javadoc

def already_commented(text: str) -> bool:
    lines = text.splitlines()
    code_lines = [l for l in lines if l.strip() and not l.strip().startswith("//") and not l.strip().startswith("*")]
    zh_lines = [l for l in lines if MARKER in l or l.strip().startswith("// 导入") or l.strip().startswith("// 声明")]
    if not code_lines:
        return True
    return len(zh_lines) >= max(5, len(code_lines) // 3)

def annotate_file(path: Path, force: bool = False) -> bool:
    text = path.read_text(encoding="utf-8")
    if not force and already_commented(text):
        return False
    lines = text.splitlines(keepends=True)
    out: list[str] = []
    in_block = False
    in_javadoc = False
    changed = False
    for line in lines:
        in_block, in_javadoc = _block_state(line, in_block, in_javadoc)
        comment = _comment_for_line(line, in_block, in_javadoc)
        if comment and MARKER not in line:
            indent = re.match(r"^(\s*)", line).group(1)
            prev = out[-1] if out else ""
            if MARKER not in prev:
                out.append(f"{indent}{MARKER} {comment}\n")
                changed = True
        out.append(line)
    if not changed:
        return False
    new_text = "".join(out)
    path.write_text(new_text, encoding="utf-8")
    return True

def collect_java_files(roots: list[Path]) -> list[Path]:
    files: list[Path] = []
    for root in roots:
        if root.is_file() and root.suffix == ".java":
            files.append(root)
        elif root.is_dir():
            files.extend(root.rglob("*.java"))
    return sorted({p.resolve() for p in files if "src/main/java" in str(p).replace("\\", "/")})

def main(argv: list[str]) -> int:
    args = argv[1:]
    force = "--force" in args
    args = [a for a in args if a != "--force"]
    if not args:
        print("Usage: add_zh_comments_java.py [--force] <file-or-dir> ...")
        return 1
    roots = [Path(a) for a in args]
    files = collect_java_files(roots)
    changed = 0
    for p in files:
        if annotate_file(p, force=force):
            changed += 1
            print(p)
    print(f"DONE {changed}/{len(files)}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
