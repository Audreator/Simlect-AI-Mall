# 全项目注释与调用链文档

## 覆盖范围（372+ 个 MODULE.md）

| 层级 | 位置 | 数量级 |
|------|------|--------|
| Simlect-agent Python | `app/*/MODULE.md` 每个包 + 子包 | ~25 |
| ragent-python | `app/*/MODULE.md` 每个包 + 子包 | ~30 |
| Simlect Java | 每个微服务 `MODULE.md` + **每个 Java 包** `MODULE.md` | ~200+ |
| Ragent Java | bootstrap/infra-ai/mcp-server 各包 | ~50+ |
| Simlect-web 前端 | `src/views|stores|components|api|.../MODULE.md` | ~30+ |
| Simlect-admin 前端 | 同上 | ~15+ |
| Ragent React 前端 | `frontend/src/*/MODULE.md` | ~20+ |

## 每个 MODULE.md 包含

1. **模块职责**（中文 + Java/Spring 类比）
2. **核心类/文件列表**
3. **逐步调用链**（A → B → C）
4. **Mermaid 调用链图**
5. **外部依赖**

## 代码内注释

- Python：`# [zh]` 逐行 + 文件头 docstring 调用链
- Java：`// [zh]` 逐行 + 类 JavaDoc 调用链（AgentInternal 等）
- Vue/React：`<!-- [zh] -->` / `// [zh]`

## 总览文档

- **全栈 Mermaid 图**：`Simlect-agent/docs/ARCHITECTURE_CALL_CHAINS.md`

## 重新生成全部模块文档

```powershell
#  handcrafted 模块（Agent/Ragent 核心包）
python Simlect-backend/Simlect-agent/tools/generate_module_docs.py

# 扫描补全：Java 每个包、Python 子包、前端每个目录
python Simlect-backend/Simlect-agent/tools/generate_all_modules.py

# 强制覆盖已有 MODULE.md
python Simlect-backend/Simlect-agent/tools/generate_all_modules.py --force
```

## 逐行 [zh] 注释（全仓库批处理）

| 语言 | 标记 | 范围 |
|------|------|------|
| Python | `# [zh]` 或行尾 `# 中文` | Simlect-agent、ragent-python 全部 .py |
| Java | `// [zh]` | Simlect-backend + ragent 共 1074 个 .java |
| Vue 模板 | `<!-- [zh] -->` | 每个完整标签行 |
| Vue script / TS / TSX | `// [zh]` | 全部前端脚本 |
| Vue style | `/* [zh] */` | 样式段 |

Vue **多行标签属性行**不能在属性之间写 `<!-- -->`（语法非法），已在开始标签上一行注释整个元素。

```powershell
python Simlect-backend/Simlect-agent/tools/dedupe_comments.py      # 去重
python Simlect-backend/Simlect-agent/tools/ensure_full_comments.py # 补全
python Simlect-backend/Simlect-agent/tools/fix_vue_comments.py     # 修复 Vue
python Simlect-backend/Simlect-agent/tools/audit_comments.py       # 审计
```

