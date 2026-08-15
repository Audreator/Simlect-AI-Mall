// 声明当前类所在的包名，用户模块内部 Agent 接口层
package com.simlect.controller.internal;

// 导入控制器基类，提供统一成功响应封装
import com.simlect.controller.ABaseController;
// 导入用户浏览历史持久化实体
import com.simlect.entity.po.UserBrowseHistory;
// 导入简单分页参数类
import com.simlect.entity.query.SimplePage;
// 导入用户浏览历史查询条件类
import com.simlect.entity.query.UserBrowseHistoryQuery;
// 导入统一 API 响应包装类
import com.simlect.entity.vo.ResponseVO;
// 导入用户浏览历史 MyBatis Mapper
import com.simlect.mappers.UserBrowseHistoryMapper;
// 导入字符串工具类（判空等）
import com.simlect.utils.StringTools;
// 导入 Jakarta 依赖注入注解，Spring 自动注入 Bean
import jakarta.annotation.Resource;
// 导入 Spring POST 请求映射注解
import org.springframework.web.bind.annotation.PostMapping;
// 导入 Spring 请求体 JSON 绑定注解
import org.springframework.web.bind.annotation.RequestBody;
// 导入 Spring 类/方法 URL 路径前缀注解
import org.springframework.web.bind.annotation.RequestMapping;
// 导入 Spring REST 控制器注解（@Controller + @ResponseBody）
import org.springframework.web.bind.annotation.RestController;

// 导入 HashMap，用于构造单键值响应
import java.util.HashMap;
// 导入 List 接口
import java.util.List;
// 导入 Map 接口
import java.util.Map;

/**
 * 用户 Agent 内部接口控制器。
 * <p>
 * 供 Simlect-agent 调用，提供与用户行为相关的只读查询（如最近浏览商品）。
 * </p>
 */
// @RestController：注册为 Spring MVC REST 控制器，响应体自动转 JSON
@RestController
// @RequestMapping：本控制器所有接口的 URL 前缀为 /internal/user/agent
@RequestMapping("/internal/user/agent")
// 继承 ABaseController，复用 getSuccessResponseVO 等方法
public class UserAgentInternalController extends ABaseController {

    // @Resource：按名称/类型注入 UserBrowseHistoryMapper Bean
    @Resource
    // 用户浏览历史数据访问对象，操作 browse_history 相关表
    private UserBrowseHistoryMapper<UserBrowseHistory, UserBrowseHistoryQuery> userBrowseHistoryMapper;

    /**
     * 查询用户最近一次浏览的商品 ID。
     * <p>Agent 可用于「继续看刚才的商品」等推荐场景。</p>
     *
     * @param body 请求体，需包含 userId
     * @return 含 productId 的 Map；无记录时 data 为 null
     */
    // @PostMapping：将方法映射到 POST /internal/user/agent/latestBrowseProductId
    @PostMapping("/latestBrowseProductId")
    // @RequestBody：将 HTTP 请求体 JSON 反序列化为 Map
    public ResponseVO<Map<String, String>> latestBrowseProductId(@RequestBody Map<String, Object> body) {
        // 从 body 提取 userId；body 或 userId 为 null 时结果为 null
        String userId = body == null || body.get("userId") == null ? null : String.valueOf(body.get("userId"));
        // userId 为空则无法查询，返回 null 数据
        if (StringTools.isEmpty(userId)) {
            return getSuccessResponseVO(null);
        }
        // 构造浏览历史查询条件
        UserBrowseHistoryQuery q = new UserBrowseHistoryQuery();
        // 限定指定用户
        q.setUserId(userId);
        // 按浏览时间倒序，u 为 Mapper 中表别名
        q.setOrderBy("u.browse_time desc");
        // 只取最新 1 条记录
        q.setSimplePage(new SimplePage(0, 1));
        // 执行查询
        List<UserBrowseHistory> list = userBrowseHistoryMapper.selectList(q);
        // 无记录或首条无 productId 则返回 null
        if (list == null || list.isEmpty() || StringTools.isEmpty(list.get(0).getProductId())) {
            return getSuccessResponseVO(null);
        }
        // 构造仅含 productId 的响应 Map
        Map<String, String> data = new HashMap<>();
        // 写入最近浏览的商品 ID
        data.put("productId", list.get(0).getProductId());
        // 封装成功响应并返回
        return getSuccessResponseVO(data);
    }
}
