package com.simlect.controller.admin;

import java.util.List;

import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.entity.query.ProductSkuQuery;
import com.simlect.entity.po.ProductSku;
import com.simlect.entity.vo.ResponseVO;
import com.simlect.biz.ProductSkuService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@RestController("productSkuController")
@RequestMapping("/admin/productSku")
public class ProductSkuController extends com.simlect.controller.admin.ABaseController{

	@Resource
	private ProductSkuService productSkuService;

	@PostMapping("/loadDataList")
	public ResponseVO loadDataList(ProductSkuQuery query){
		return getSuccessResponseVO(productSkuService.findListByPage(query));
	}

	@PostMapping("/add")
	public ResponseVO add(ProductSku bean) {
		productSkuService.add(bean);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/addBatch")
	public ResponseVO addBatch(@RequestBody List<ProductSku> listBean) {
		productSkuService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/addOrUpdateBatch")
	public ResponseVO addOrUpdateBatch(@RequestBody List<ProductSku> listBean) {
		productSkuService.addOrUpdateBatch(listBean);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/getProductSkuByProductIdAndPropertyValueIdHash")
	public ResponseVO getProductSkuByProductIdAndPropertyValueIdHash(String productId,String propertyValueIdHash) {
		return getSuccessResponseVO(productSkuService.getProductSkuByProductIdAndPropertyValueIdHash(productId,propertyValueIdHash));
	}

	@PostMapping("/updateProductSkuByProductIdAndPropertyValueIdHash")
	public ResponseVO updateProductSkuByProductIdAndPropertyValueIdHash(ProductSku bean,String productId,String propertyValueIdHash) {
		productSkuService.updateProductSkuByProductIdAndPropertyValueIdHash(bean,productId,propertyValueIdHash);
		return getSuccessResponseVO(null);
	}

@AdminSensitiveConfirm
    	@PostMapping("/deleteProductSkuByProductIdAndPropertyValueIdHash")
	public ResponseVO deleteProductSkuByProductIdAndPropertyValueIdHash(String productId,String propertyValueIdHash) {
		productSkuService.deleteProductSkuByProductIdAndPropertyValueIdHash(productId,propertyValueIdHash);
		return getSuccessResponseVO(null);
	}
}
