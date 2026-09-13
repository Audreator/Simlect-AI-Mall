package com.simlect.entity.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserSignRecordDetailQuery extends BaseParam {

    private String userId;

    private String signDate;

    private String signDateStart;

    private String signDateEnd;

    private Integer signType;

    private java.util.Date createTimeStart;
}
