package com.simlect.utils;
import com.simlect.constants.Constants;
import com.simlect.exception.BusinessException;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.util.DigestUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;

public class StringTools {

    public static void checkParam(Object param) {
        try {
            Field[] fields = param.getClass().getDeclaredFields();
            Boolean notEmpty = false;
            for (Field field : fields) {
                String methodName = "get" + StringTools.upperCaseFirstLetter(field.getName());
                Method method = param.getClass().getMethod(methodName);
                Object object = method.invoke(param);
                if (object != null && object instanceof java.lang.String && !StringTools.isEmpty(object.toString())
                        || object != null && !(object instanceof java.lang.String)) {
                    notEmpty = true;
                    break;
                }
            }
            if (!notEmpty) {
                throw new BusinessException("多参数更新，删除，必须有非空条件");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("校验参数是否为空失败");
        }
    }

    public static String upperCaseFirstLetter(String field) {
        if (isEmpty(field)) {
            return field;
        }
        //如果第二个字母是大写，第一个字母不大写
        if (field.length() > 1 && Character.isUpperCase(field.charAt(1))) {
            return field;
        }
        return field.substring(0, 1).toUpperCase() + field.substring(1);
    }

    public static Boolean isEmpty(String str) {
        if (null == str || "".equals(str) || "null".equals(str) || "\u0000".equals(str)) {
            return true;
        } else if ("".equals(str.trim())) {
            return true;
        }
        return false;
    }

    public static String getRandomString(Integer count) {
        return RandomStringUtils.random(count, true, true);
    }

    public static String getFileSuffix(String fileName) {
        if (StringTools.isEmpty(fileName)) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        String suffix = fileName.substring(lastDotIndex);
        return suffix;
    }

    public static final String getRandomNumber(Integer count) {
        return RandomStringUtils.random(count, false, true);
    }

    public static String encodeByMD5(String str){
        return StringTools.isEmpty(str) ? null : DigestUtils.md5DigestAsHex(str.getBytes());
    }

    public static Boolean pathIsOK(@NotNull String sourceName) {
        return !sourceName.contains("..") ;
    }

    // 获取时间，精确到秒
    public static Date getCurrentDate() {
        return new Date();
    }

    public static String createOrderId() {
        // 生成时间yyyymmddhhmmsssss到毫秒+15位随机字符串
        return DateUtil.format(new Date(), "yyyyMMddHHmmssSSS") + getRandomString(Constants.LENGTH_15);
    }

    public static final String createPayOrderId() {
        return StringTools.getRandomNumber(Constants.LENGTH_30);
    }

    public static String createCouponId() {
        // 格式CP + yyyyMMdd + 5位随机字符串
        return "CP" + DateUtil.format(new Date(), "yyyyMMdd") + getRandomString(Constants.LENGTH_5);
    }

    public static String createUserCouponId() {
        // 30位的随机字符串
        return getRandomString(Constants.LENGTH_30);
    }

    public static String createNotificationId() {
        return "N" + getRandomString(Constants.LENGTH_30);
    }

    public static String createTradeId() {
        return "T" + DateUtil.format(new Date(), "yyyyMMddHHmmssSSS") + getRandomString(Constants.LENGTH_15);
    }
}
