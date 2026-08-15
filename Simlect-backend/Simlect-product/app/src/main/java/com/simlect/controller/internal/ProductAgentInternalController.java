// 声明当前类所在的包名，商品模块内部 Agent 接口层
package com.simlect.controller.internal;

// 导入控制器基类，提供统一响应封装
import com.simlect.controller.ABaseController;
// 导入商品状态枚举，用于筛选「在售」商品
import com.simlect.api.enums.ProductStatusEnum;
// 导入商品信息持久化实体
import com.simlect.entity.po.ProductInfo;
// 导入商品规格属性值实体
import com.simlect.entity.po.ProductPropertyValue;
// 导入商品 SKU 实体
import com.simlect.entity.po.ProductSku;
// 导入商品信息查询条件类
import com.simlect.entity.query.ProductInfoQuery;
// 导入商品规格值查询条件类
import com.simlect.entity.query.ProductPropertyValueQuery;
// 导入商品 SKU 查询条件类
import com.simlect.entity.query.ProductSkuQuery;
// 导入简单分页参数类
import com.simlect.entity.query.SimplePage;
// 导入统一 API 响应包装类
import com.simlect.entity.vo.ResponseVO;
// 导入商品信息 MyBatis Mapper
import com.simlect.mappers.ProductInfoMapper;
// 导入商品规格值 MyBatis Mapper
import com.simlect.mappers.ProductPropertyValueMapper;
// 导入商品 SKU MyBatis Mapper
import com.simlect.mappers.ProductSkuMapper;
// 导入字符串工具类
import com.simlect.utils.StringTools;
// 导入 Jakarta 依赖注入注解
import jakarta.annotation.Resource;
// 导入 Spring POST 请求映射注解
import org.springframework.web.bind.annotation.PostMapping;
// 导入 Spring 请求体绑定注解
import org.springframework.web.bind.annotation.RequestBody;
// 导入 Spring URL 路径前缀注解
import org.springframework.web.bind.annotation.RequestMapping;
// 导入 Spring REST 控制器注解
import org.springframework.web.bind.annotation.RestController;

// 导入 ArrayList 列表实现
import java.util.ArrayList;
// 导入空列表常量
import java.util.Collections;
// 导入有序 Map，保证 JSON 字段顺序
import java.util.LinkedHashMap;
// 导入 List 接口
import java.util.List;
// 导入 Map 接口
import java.util.Map;

/**
 * 商品 Agent 内部接口控制器。
 * <p>
 * 供 Simlect-agent 调用，提供在售商品搜索、商品详情（含 SKU 与规格）查询。
 * </p>
 *
 * <h3>完整调用链（Agent 侧 → 本 Controller → DB）</h3>
 * <pre>
 * 用户发消息 → Simlect-agent graph/nodes → mcp_tool_router
 *   → mcp_streamable_client → MCP 进程 mcp_server/server.py::search_products
 *   → mcp_tools_service → java_internal_client.post_json
 *   → Gateway（可选）→ 本类 /internal/product/agent/search 或 /hotSale
 *   → ProductInfoMapper.selectList → MySQL product_info
 *   → toAgentProductCard() → ResponseVO → Agent 渲染商品卡片
 * </pre>
 * <p>详见 Simlect-product/MODULE.md、Simlect-agent/app/mcp_server/MODULE.md</p>
 */
// @RestController：标记为 REST 控制器，返回值序列化为 JSON
@RestController
// @RequestMapping：类级别路径前缀 /internal/product/agent
@RequestMapping("/internal/product/agent")
// 继承基类以使用 getSuccessResponseVO
public class ProductAgentInternalController extends ABaseController {

    // @Resource：注入商品信息 Mapper，直接访问 product_info 表
    @Resource
    // 商品主表数据访问对象
    private ProductInfoMapper<ProductInfo, ProductInfoQuery> productInfoMapper;
    // @Resource：注入 SKU Mapper
    @Resource
    // 商品 SKU 数据访问对象
    private ProductSkuMapper<ProductSku, ProductSkuQuery> productSkuMapper;
    // @Resource：注入规格属性值 Mapper
    @Resource
    // 商品规格属性值数据访问对象
    private ProductPropertyValueMapper<ProductPropertyValue, ProductPropertyValueQuery> productPropertyValueMapper;

    /**
     * 搜索在售商品（支持关键词、分类、热销排序）。
     *
     * @param body 请求体：keyword、categoryId、hotSale、limit 等
     * @return 商品卡片 Map 列表
     */
    // @PostMapping：POST /internal/product/agent/searchOnSale
    @PostMapping("/searchOnSale")
    public ResponseVO<List<Map<String, Object>>> searchOnSale(@RequestBody Map<String, Object> body) {
        // 构造商品查询条件
        ProductInfoQuery query = new ProductInfoQuery();
        // 仅查询「在售」状态商品
        query.setStatus(ProductStatusEnum.ON_SALE.getStatus());
        // 读取搜索关键词
        String keyword = str(body, "keyword");
        // 非 category: 前缀的关键词用于商品名模糊搜索
        if (!StringTools.isEmpty(keyword) && !keyword.startsWith("category:")) {
            query.setProductNameFuzzy(keyword);
        }
        // 读取显式分类 ID
        String categoryId = str(body, "categoryId");
        // 关键词形如 category:xxx 时从 keyword 解析分类 ID
        if (StringTools.isEmpty(categoryId) && keyword != null && keyword.startsWith("category:")) {
            categoryId = keyword.substring("category:".length()).trim();
        }
        // 有分类 ID 则按分类过滤
        if (!StringTools.isEmpty(categoryId)) {
            query.setCategoryId(categoryId);
        }
        // 判断是否请求热销排序：布尔 true 或字符串 "true"
        boolean hotSale = Boolean.TRUE.equals(body.get("hotSale"))
                || "true".equalsIgnoreCase(String.valueOf(body.get("hotSale")));
        // 数据库列为 total_sale（非 sales）；Mapper 中表别名为 p
        if (hotSale) {
            // 热销：按总销量降序
            query.setOrderBy("p.total_sale desc");
        } else {
            // 默认：按创建时间降序（新品优先）
            query.setOrderBy("p.create_time desc");
        }
        // 解析 limit，默认 20 条
        int limit = intVal(body.get("limit"), 20);
        // 下限保护：至少 1 条
        if (limit < 1) {
            limit = 1;
        }
        // 上限保护：最多 50 条，防止 Agent 拉取过多数据
        if (limit > 50) {
            limit = 50;
        }
        // 设置分页：从第 0 页开始，每页 limit 条
        query.setSimplePage(new SimplePage(0, limit));
        // 执行列表查询
        List<ProductInfo> list = productInfoMapper.selectList(query);
        // 准备 Agent 卡片结果列表
        List<Map<String, Object>> result = new ArrayList<>();
        // 非空则逐条转换为卡片 Map
        if (list != null) {
            for (ProductInfo p : list) {
                result.add(toAgentProductCard(p));
            }
        }
        // 返回成功响应及商品列表
        return getSuccessResponseVO(result);
    }

    /**
     * 查询单个商品详情，含 SKU 列表与规格属性值。
     *
     * @param body 请求体，需包含 productId
     * @return 详情 Map；不存在时 data 为 null
     */
    // @PostMapping：POST /internal/product/agent/getDetail
    @PostMapping("/getDetail")
    public ResponseVO<Map<String, Object>> getDetail(@RequestBody Map<String, Object> body) {
        // 提取商品 ID
        String productId = str(body, "productId");
        // ID 为空返回 null
        if (StringTools.isEmpty(productId)) {
            return getSuccessResponseVO(null);
        }
        // 按主键查询商品
        ProductInfo p = productInfoMapper.selectByProductId(productId);
        // 商品不存在
        if (p == null) {
            return getSuccessResponseVO(null);
        }
        // 先填充基础卡片字段
        Map<String, Object> m = toAgentProductCard(p);
        // 补充状态字段
        m.put("status", p.getStatus());
        // 最高售价
        m.put("maxPrice", p.getMaxPrice());
        // 商品描述（Agent 字段名 description）
        m.put("description", p.getProductDesc());
        // 兼容字段 productDesc
        m.put("productDesc", p.getProductDesc());

        // 构造 SKU 查询条件
        ProductSkuQuery skuQuery = new ProductSkuQuery();
        // 限定当前商品
        skuQuery.setProductId(productId);
        // 按 sort 升序排列规格组合
        skuQuery.setOrderBy("sort asc");
        // 查询 SKU 列表
        List<ProductSku> skus = productSkuMapper.selectList(skuQuery);
        // 写入 skus 字段，null 时用空列表
        m.put("skus", skus == null ? Collections.emptyList() : skus);

        // 构造规格属性值查询条件
        ProductPropertyValueQuery pvQuery = new ProductPropertyValueQuery();
        // 限定当前商品
        pvQuery.setProductId(productId);
        // 查询全部规格值
        List<ProductPropertyValue> pvs = productPropertyValueMapper.selectList(pvQuery);
        // 写入 propertyValues 字段
        m.put("propertyValues", pvs == null ? Collections.emptyList() : pvs);
        // 返回完整详情
        return getSuccessResponseVO(m);
    }

    /**
     * 将商品实体转换为 Agent 侧商品卡片 Map。
     * <p>totalSale 为主字段；sales 为兼容旧客户端的别名。</p>
     *
     * @param p 商品实体
     * @return 卡片字段 Map
     */
    // 私有静态方法：统一卡片字段结构
    private static Map<String, Object> toAgentProductCard(ProductInfo p) {
        // 有序 Map
        Map<String, Object> m = new LinkedHashMap<>();
        // 商品 ID
        m.put("productId", p.getProductId());
        // 商品名称
        m.put("productName", p.getProductName());
        // 封面图 URL
        m.put("cover", p.getCover());
        // 最低售价
        m.put("minPrice", p.getMinPrice());
        // 分类 ID
        m.put("categoryId", p.getCategoryId());
        // 总销量（标准字段名）
        m.put("totalSale", p.getTotalSale());
        // 销量别名，与 totalSale 同值
        m.put("sales", p.getTotalSale());
        // 返回卡片 Map
        return m;
    }

    /**
     * 从请求体 Map 安全读取字符串字段。
     * <p>字符串 "null" 会被转为 Java null。</p>
     *
     * @param body 请求体
     * @param key  字段名
     * @return 字符串值或 null
     */
    // 静态工具：解析 body 字符串参数
    private static String str(Map<String, Object> body, String key) {
        // body 或值为 null
        if (body == null || body.get(key) == null) {
            return null;
        }
        // 转为字符串
        String v = String.valueOf(body.get(key));
        // JSON 中字面量 "null" 视为真 null
        return "null".equals(v) ? null : v;
    }

    /**
     * 将 Object 解析为 int，失败时返回默认值。
     *
     * @param v   待解析值
     * @param def 默认值
     * @return 整数值
     */
    // 静态工具：解析 limit 等数值
    private static int intVal(Object v, int def) {
        // null 用默认值
        if (v == null) {
            return def;
        }
        try {
            // 字符串转 int
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            // 解析失败回退默认值
            return def;
        }
    }
}
