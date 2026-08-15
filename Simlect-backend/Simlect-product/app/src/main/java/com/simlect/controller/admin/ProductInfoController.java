package com.simlect.controller.admin;

import java.util.List;

import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.entity.dto.ProductSaveDTO;
import com.simlect.entity.query.ProductInfoQuery;
import com.simlect.entity.po.ProductInfo;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.entity.vo.ResponseVO;
import com.simlect.biz.ProductInfoService;
import com.simlect.biz.ProductSkuService;
import com.simlect.valid.Create;
import com.simlect.valid.Update;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@RestController("productInfoController")
@RequestMapping("/admin/productInfo")
public class ProductInfoController extends com.simlect.controller.admin.ABaseController{

	@Resource
	private ProductInfoService productInfoService;

	@Resource
	private ProductSkuService productSkuService;

	@PostMapping("/loadDataList")
	public ResponseVO loadDataList(ProductInfoQuery query){
		return getSuccessResponseVO(productInfoService.findListByPage(query));
	}

	@PostMapping("/addProduct")
	public ResponseVO addProduct(@RequestBody @Validated(Create.class) ProductSaveDTO productSaveDTO) {
		productInfoService.saveProduct(productSaveDTO);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/updateProduct")
	@AdminSensitiveConfirm
	public ResponseVO updateProduct(@RequestBody @Validated(Update.class) ProductSaveDTO productSaveDTO) {
		productInfoService.saveProduct(productSaveDTO);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/addBatch")
	public ResponseVO addBatch(@RequestBody List<ProductInfo> listBean) {
		productInfoService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/addOrUpdateBatch")
	public ResponseVO addOrUpdateBatch(@RequestBody List<ProductInfo> listBean) {
		productInfoService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/getProductInfoByProductId")
	public ResponseVO getProductInfoByProductId(String productId) {
		return getSuccessResponseVO(productInfoService.getProductInfoByProductId(productId));
	}

    @PostMapping("/updateProductInfoByProductId")
    @AdminSensitiveConfirm
    public ResponseVO updateProductInfoByProductId(ProductInfo bean,String productId) {
		productInfoService.updateProductInfoByProductId(bean,productId);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/deleteProductInfoByProductId")
	@AdminSensitiveConfirm
	public ResponseVO deleteProductInfoByProductId(String productId) {
		productInfoService.deleteProductInfoByProductId(productId);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/loadProduct")
	public ResponseVO loadProduct(String productNameFuzzy,
								  Integer pageNo,
								  Integer pageSize,
								  String categoryIdOrPCategoryId,
								  Integer status,
								  Integer commendType) {
		ProductInfoQuery query = new ProductInfoQuery();
		query.setProductNameFuzzy(productNameFuzzy);
		query.setPageNo(pageNo);
		query.setPageSize(pageSize);
		query.setCategoryIdOrPCategoryId(categoryIdOrPCategoryId);
		query.setStatus(status);
		query.setCommendType(commendType);
		query.setOrderBy("create_time desc");
		PaginationResultVO resultVO = productInfoService.findListByPage4ListVO(query);
		return getSuccessResponseVO(resultVO);
	}

	@PostMapping("/getProductInfo")
	public ResponseVO getProductInfo(@NotEmpty String productId) {
		return getSuccessResponseVO(productInfoService.getProduct4VOByProductId(productId));
	}

	@PostMapping("/updateSkuStock")
	@AdminSensitiveConfirm
	public ResponseVO updateSkuStock(@NotEmpty String productId, @NotEmpty String propertyValueIdHash, @NotNull Integer changeStock) {
		productSkuService.updateStock(productId, propertyValueIdHash, changeStock);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/updateProductStatus")
	@AdminSensitiveConfirm
	public ResponseVO updateProductStatus(@NotNull String productId, @NotNull Integer status) {
		productInfoService.updateProductStatus(productId,status);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/deleteProduct")
	@AdminSensitiveConfirm
	public ResponseVO deleteProduct(@NotNull String productId) {
		productInfoService.deleteProduct(productId);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/commendProduct")
	public ResponseVO commendProduct(@NotNull String productId, @NotNull Integer commendType) {
		productInfoService.commendProduct(productId,commendType);
		return getSuccessResponseVO(null);
	}
}
