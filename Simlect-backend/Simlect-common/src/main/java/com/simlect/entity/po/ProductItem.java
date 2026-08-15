package com.simlect.entity.po;

import com.simlect.constants.Constants;
import com.simlect.utils.StringTools;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public class ProductItem {
    @NotEmpty
    private String productId;
    @NotEmpty
    private String propertyValueIds;
    @NotNull
    @Min(1)
    @Max(Constants.ORDER_MAX_BUY_COUNT_PER_SKU)
    private Integer buyCount;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getPropertyValueIds() {
        return propertyValueIds;
    }

    public void setPropertyValueIds(String propertyValueIds) {
        this.propertyValueIds = propertyValueIds;
        if (!StringTools.isEmpty(propertyValueIds)) {
            this.propertyValueIdHash = StringTools.encodeByMD5(propertyValueIds);
        }
    }

    public Integer getBuyCount() {
        return buyCount;
    }

    public void setBuyCount(Integer buyCount) {
        this.buyCount = buyCount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    private String remark;

    private String propertyValueIdHash;

    public String getPropertyValueIdHash() {
        return propertyValueIdHash;
    }

    public void setPropertyValueIdHash(String propertyValueIdHash) {
        this.propertyValueIdHash = propertyValueIdHash;
    }
}
