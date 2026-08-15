package com.simlect.api.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * PayChannelEnum 支付渠道枚举解析单元测试。
 */
class PayChannelEnumTest {

    @Test
    void getByPayScene_findsPcAndWap() {
        assertSame(PayChannelEnum.ALIPAY_PC, PayChannelEnum.getByPayScene("alipay_pc"));
        assertSame(PayChannelEnum.ALIPAY_WAP, PayChannelEnum.getByPayScene("alipay_wap"));
    }

    @Test
    void getByPayScene_unknownOrEmpty_returnsNull() {
        assertNull(PayChannelEnum.getByPayScene("wechat"));
        assertNull(PayChannelEnum.getByPayScene(""));
        assertNull(PayChannelEnum.getByPayScene(null));
    }

    @Test
    void resolve_sceneOrChannelBothWork() {
        assertSame(PayChannelEnum.ALIPAY_PC, PayChannelEnum.resolve("alipay_pc"));
        assertSame(PayChannelEnum.ALIPAY_PC, PayChannelEnum.resolve("alipay"));
        assertSame(PayChannelEnum.ALIPAY_WAP, PayChannelEnum.resolve("alipay_wap"));
    }

    @Test
    void resolve_unknownOrEmpty_returnsNull() {
        assertNull(PayChannelEnum.resolve("wechat"));
        assertNull(PayChannelEnum.resolve(""));
        assertNull(PayChannelEnum.resolve(null));
    }

    @Test
    void metadata_isConsistent() {
        assertEquals("payChannel4Alipay", PayChannelEnum.ALIPAY_PC.getBeanName());
        assertEquals("payChannel4Alipay", PayChannelEnum.ALIPAY_WAP.getBeanName());
        assertEquals("支付宝电脑网站支付", PayChannelEnum.ALIPAY_PC.getDesc());
    }
}
