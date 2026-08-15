package com.simlect.entity.enums;

import com.simlect.utils.StringTools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum PromptTypeEnum {

    GLOBAL("global", "prompt/global.txt", "全局规则"),
    AGENT("agent", "prompt/agent.txt", "统一 Agent 系统词（兼容）"),
    USER_INTENT("user_intent", "prompt/user_intent.txt", "意图分类"),
    CHAT("chat", "prompt/chat.txt", "闲聊/政策问答"),
    PRODUCT_CONSULT("product_consult", "prompt/product_consult.txt", "商品咨询"),
    PRODUCT_SEARCH("product_search", "prompt/product_search.txt", "商品搜索"),
    QUERY_ORDER("query_order", "prompt/query_order.txt", "查询订单"),
    QUERY_LOGISTICS("query_logistics", "prompt/query_logistics.txt", "查询物流"),
    QUERY_COUPON("query_coupon", "prompt/query_coupon.txt", "查询优惠券"),
    QUERY_COMMENT("query_comment", "prompt/query_comment.txt", "查看评价"),
    PRODUCT_REVIEW("product_review", "prompt/product_review.txt", "提交评价"),
    RECOMMENT("recomment", "prompt/recomment.txt", "追评"),
    REFUND("refund", "prompt/refund.txt", "退款"),
    CONFIRM_RECEIPT("confirm_receipt", "prompt/confirm_receipt.txt", "确认收货"),
    CANCEL_ORDER("cancel_order", "prompt/cancel_order.txt", "取消订单"),
    COMPRESS("compress", "prompt/compress.txt", "会话历史压缩"),

    ;

    private String key;
    private String prompt;
    private String desc;

    PromptTypeEnum(String key, String txtPath, String desc) {
        this.key = key;
        this.prompt = loadPrompt(txtPath);
        this.desc = desc;
    }

    public String getKey() {
        return key;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getDesc() {
        return desc;
    }

    private static String loadPrompt(String path) {
        if (StringTools.isEmpty(path)) {
            return "";
        }
        try (var is = PromptTypeEnum.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                return "";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取提示词文件失败: " + path, e);
        }
    }

    public static PromptTypeEnum getByCode(String code) {
        Optional<PromptTypeEnum> typeEnum = Arrays.stream(PromptTypeEnum.values()).filter(value -> value.toString().equals(code)).findFirst();
        return typeEnum == null || typeEnum.isEmpty() ? null : typeEnum.get();
    }

    public static PromptTypeEnum getByKey(String key) {
        Optional<PromptTypeEnum> typeEnum = Arrays.stream(PromptTypeEnum.values()).filter(value -> value.getKey().equals(key)).findFirst();
        return typeEnum == null || typeEnum.isEmpty() ? null : typeEnum.get();
    }

    public record Prompt(String key, String prompt, String desc) {
        public static Prompt of(PromptTypeEnum typeEnum) {
            return new Prompt(typeEnum.getKey(), typeEnum.getPrompt(), typeEnum.getDesc());
        }
    }

    public static List<Prompt> getPrompts() {
        return Arrays.stream(PromptTypeEnum.values()).filter(value -> !StringTools.isEmpty(value.getPrompt())).map(Prompt::of).toList();
    }
}
