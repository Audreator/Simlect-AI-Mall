// 声明当前类所在的包名，优惠券模块内部 Agent 接口层
package com.simlect.controller.internal;

// 导入控制器基类，提供统一 API 响应方法
import com.simlect.controller.ABaseController;
// 导入优惠券模板（折扣券定义）持久化实体
import com.simlect.entity.po.DiscountCoupon;
// 导入用户领取的优惠券实例实体
import com.simlect.entity.po.UserCoupon;
// 导入优惠券模板查询条件类
import com.simlect.entity.query.DiscountCouponQuery;
// 导入用户优惠券查询条件类
import com.simlect.entity.query.UserCouponQuery;
// 导入统一 API 响应包装类
import com.simlect.entity.vo.ResponseVO;
// 导入优惠券模板 MyBatis Mapper
import com.simlect.mappers.DiscountCouponMapper;
// 导入用户优惠券 MyBatis Mapper
import com.simlect.mappers.UserCouponMapper;
// 导入字符串工具类
import com.simlect.utils.StringTools;
// 导入 Jakarta @Resource 依赖注入注解
import jakarta.annotation.Resource;
// 导入 Spring POST 映射注解
import org.springframework.web.bind.annotation.PostMapping;
// 导入 Spring 请求体绑定注解
import org.springframework.web.bind.annotation.RequestBody;
// 导入 Spring URL 路径前缀注解
import org.springframework.web.bind.annotation.RequestMapping;
// 导入 Spring REST 控制器注解
import org.springframework.web.bind.annotation.RestController;

// 导入 ArrayList 可变列表
import java.util.ArrayList;
// 导入空列表工具
import java.util.Collections;
// 导入有序 Map，保证 JSON 字段顺序稳定
import java.util.LinkedHashMap;
// 导入 List 接口
import java.util.List;
// 导入 Map 接口
import java.util.Map;

/**
 * 优惠券 Agent 内部接口控制器。
 * <p>
 * 供 Simlect-agent 调用，查询用户持有的优惠券及关联的券模板信息（面额、门槛、有效期等）。
 * </p>
 */
// @RestController：Spring 组件，处理 HTTP 请求并将返回值序列化为 JSON
@RestController
// @RequestMapping：类级别路径前缀 /internal/coupon/agent
@RequestMapping("/internal/coupon/agent")
// 继承基类以使用 getSuccessResponseVO
public class CouponAgentInternalController extends ABaseController {

    // @Resource：注入用户优惠券 Mapper Bean
    @Resource
    // 用户券实例表数据访问对象
    private UserCouponMapper<UserCoupon, UserCouponQuery> userCouponMapper;
    // @Resource：注入优惠券模板 Mapper Bean
    @Resource
    // 优惠券定义表数据访问对象
    private DiscountCouponMapper<DiscountCoupon, DiscountCouponQuery> discountCouponMapper;

    /**
     * 列出指定用户拥有的全部优惠券（含模板详情）。
     *
     * @param body 请求体，需包含 userId
     * @return 用户券 Map 列表，每项合并 UserCoupon 与 DiscountCoupon 字段
     */
    // @PostMapping：POST /internal/coupon/agent/listUserCoupons
    @PostMapping("/listUserCoupons")
    public ResponseVO<List<Map<String, Object>>> listUserCoupons(@RequestBody Map<String, Object> body) {
        // 从请求体解析 userId
        String userId = body == null || body.get("userId") == null ? null : String.valueOf(body.get("userId"));
        // userId 为空返回空列表
        if (StringTools.isEmpty(userId)) {
            return getSuccessResponseVO(Collections.emptyList());
        }
        // 构造用户券查询条件
        UserCouponQuery q = new UserCouponQuery();
        // 限定用户
        q.setUserId(userId);
        // 查询该用户全部券实例
        List<UserCoupon> list = userCouponMapper.selectList(q);
        // 准备 Agent 响应列表
        List<Map<String, Object>> result = new ArrayList<>();
        // Mapper 返回 null 时直接返回空结果
        if (list == null) {
            return getSuccessResponseVO(result);
        }
        // 遍历每张用户券，关联查询券模板并组装 Map
        for (UserCoupon uc : list) {
            // 每条记录使用有序 Map
            Map<String, Object> m = new LinkedHashMap<>();
            // 用户券实例主键
            m.put("userCouponId", uc.getUserCouponId());
            // 所属用户 ID
            m.put("userId", uc.getUserId());
            // 关联的优惠券模板 ID
            m.put("couponId", uc.getCouponId());
            // 用户券状态（未使用/已用/过期等）
            m.put("status", uc.getStatus());
            // 根据 couponId 查询优惠券模板定义
            DiscountCoupon dc = discountCouponMapper.selectByCouponId(uc.getCouponId());
            // 模板存在则填充展示字段
            if (dc != null) {
                // 券名称
                m.put("couponName", dc.getCouponName());
                // 券类型（满减/折扣等）
                m.put("couponType", dc.getCouponType());
                // 优惠金额
                m.put("discountAmount", dc.getDiscountAmount());
                // 使用门槛（Agent 字段 minAmount）
                m.put("minAmount", dc.getThresholdAmount());
                // 门槛金额（与 minAmount 同值，兼容不同字段名）
                m.put("thresholdAmount", dc.getThresholdAmount());
                // 有效期截止时间
                m.put("validEndTime", dc.getValidEndTime());
            }
            // 加入结果列表
            result.add(m);
        }
        // 返回用户优惠券列表
        return getSuccessResponseVO(result);
    }
}
