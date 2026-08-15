package com.simlect.entity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResponseCodeEnumTest {

    @Test
    void allConstants_exposeCodeAndMsg() {
        assertEquals(200, ResponseCodeEnum.CODE_200.getCode());
        assertEquals("请求成功", ResponseCodeEnum.CODE_200.getMsg());
        assertEquals(404, ResponseCodeEnum.CODE_404.getCode());
        assertEquals(600, ResponseCodeEnum.CODE_600.getCode());
        assertEquals(601, ResponseCodeEnum.CODE_601.getCode());
        assertEquals(602, ResponseCodeEnum.CODE_602.getCode());
        assertEquals(603, ResponseCodeEnum.CODE_603.getCode());
        assertEquals(604, ResponseCodeEnum.CODE_604.getCode());
        assertEquals(605, ResponseCodeEnum.CODE_605.getCode());
        assertEquals(500, ResponseCodeEnum.CODE_500.getCode());
        assertEquals("服务器返回错误，请联系管理员", ResponseCodeEnum.CODE_500.getMsg());
        assertEquals(901, ResponseCodeEnum.CODE_901.getCode());
        assertEquals("登录超时", ResponseCodeEnum.CODE_901.getMsg());
    }
}
