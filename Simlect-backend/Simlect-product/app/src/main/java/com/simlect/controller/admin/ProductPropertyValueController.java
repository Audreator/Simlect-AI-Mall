package com.simlect.controller.admin;

import java.util.List;

import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.entity.query.ProductPropertyValueQuery;
import com.simlect.entity.po.ProductPropertyValue;
import com.simlect.entity.vo.ResponseVO;
import com.simlect.biz.ProductPropertyValueService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@RestController("productPropertyValueController")
@RequestMapping("/admin/productPropertyValue")
public class ProductPropertyValueController extends com.simlect.controller.admin.ABaseController{

	@Resource
	private ProductPropertyValueService productPropertyValueService;

	@PostMapping("/loadDataList")
	public ResponseVO loadDataList(ProductPropertyValueQuery query){
		return getSuccessResponseVO(productPropertyValueService.findListByPage(query));
	}

	@PostMapping("/add")
	public ResponseVO add(ProductPropertyValue bean) {
		productPropertyValueService.add(bean);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/addBatch")
	public ResponseVO addBatch(@RequestBody List<ProductPropertyValue> listBean) {
		productPropertyValueService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/addOrUpdateBatch")
	public ResponseVO addOrUpdateBatch(@RequestBody List<ProductPropertyValue> listBean) {
		productPropertyValueService.addOrUpdateBatch(listBean);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/getProductPropertyValueByProductIdAndPropertyValueId")
	public ResponseVO getProductPropertyValueByProductIdAndPropertyValueId(String productId,String propertyValueId) {
		return getSuccessResponseVO(productPropertyValueService.getProductPropertyValueByProductIdAndPropertyValueId(productId,propertyValueId));
	}

	@PostMapping("/updateProductPropertyValueByProductIdAndPropertyValueId")
	public ResponseVO updateProductPropertyValueByProductIdAndPropertyValueId(ProductPropertyValue bean,String productId,String propertyValueId) {
		productPropertyValueService.updateProductPropertyValueByProductIdAndPropertyValueId(bean,productId,propertyValueId);
		return getSuccessResponseVO(null);
	}

@AdminSensitiveConfirm
    	@PostMapping("/deleteProductPropertyValueByProductIdAndPropertyValueId")
	public ResponseVO deleteProductPropertyValueByProductIdAndPropertyValueId(String productId,String propertyValueId) {
		productPropertyValueService.deleteProductPropertyValueByProductIdAndPropertyValueId(productId,propertyValueId);
		return getSuccessResponseVO(null);
	}
}
