package com.simlect.entity.enums;

public enum ResponseCodeEnum {
    CODE_200(200, "请求成功"), CODE_404(404, "请求地址不存在"), CODE_600(600, "请求参数错误"), CODE_601(601, "信息已经存在"), CODE_602(602, "文件不存在"), CODE_603(603, "数据转换失败"), CODE_604(604,
            "接口请求超时"), CODE_605(605, "接口请求失败"), CODE_500(500, "服务器返回错误，请联系管理员"), CODE_901(901, "登录超时");

    private Integer code;

    private String msg;

    ResponseCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
