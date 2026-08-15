package com.simlect.entity.query;

import com.simlect.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BaseParam.orderBy 结构白名单：防 SQL 注入。
 */
class BaseParamTest {

    private final BaseParam param = new BaseParam();

    @Test
    void setOrderBy_nullClears() {
        param.setOrderBy("create_time desc");
        param.setOrderBy(null);
        assertNull(param.getOrderBy());
    }

    @Test
    void setOrderBy_simpleColumnAndDirection_ok() {
        param.setOrderBy("id desc");
        assertEquals("id desc", param.getOrderBy());
        param.setOrderBy("create_time asc");
        assertEquals("create_time asc", param.getOrderBy());
        param.setOrderBy("order_time");
        assertEquals("order_time", param.getOrderBy());
    }

    @Test
    void setOrderBy_multiColumns_ok() {
        param.setOrderBy("statistics_date desc, data_type asc");
        assertEquals("statistics_date desc, data_type asc", param.getOrderBy());
    }

    @Test
    void setOrderBy_tableAliasPrefix_ok() {
        param.setOrderBy("o.order_time desc");
        assertEquals("o.order_time desc", param.getOrderBy());
    }

    @Test
    void setOrderBy_coalesceAllowed_onlyForInternalFixedPattern() {
        param.setOrderBy("COALESCE(recomment_time, comment_time) desc");
        assertEquals("COALESCE(recomment_time, comment_time) desc", param.getOrderBy());
    }

    @Test
    void setOrderBy_sqlInjection_rejected() {
        assertThrows(BusinessException.class, () -> param.setOrderBy("id; drop table order_info"));
        assertThrows(BusinessException.class, () -> param.setOrderBy("1;select 1"));
        assertThrows(BusinessException.class, () -> param.setOrderBy("id desc--"));
        assertThrows(BusinessException.class, () -> param.setOrderBy("id desc /*x*/"));
        assertThrows(BusinessException.class, () -> param.setOrderBy("(select 1)"));
        assertThrows(BusinessException.class, () -> param.setOrderBy("`id`"));
        assertThrows(BusinessException.class, () -> param.setOrderBy("id'"));
        assertThrows(BusinessException.class, () -> param.setOrderBy("sleep(1)"));
        assertThrows(BusinessException.class, () -> param.setOrderBy("case when 1=1 then id end"));
    }

    @Test
    void setOrderBy_oversized_rejected() {
        StringBuilder sb = new StringBuilder("id,");
        for (int i = 0; i < 50; i++) {
            sb.append("c").append(i).append(",");
        }
        assertThrows(BusinessException.class, () -> param.setOrderBy(sb.toString()));
    }
}
