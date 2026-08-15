package com.simlect.controller.admin;

import java.util.List;

import com.simlect.annotation.AdminSensitiveConfirm;
import com.simlect.entity.po.SysProductProperty;
import com.simlect.entity.query.SysCategoryQuery;
import com.simlect.entity.po.SysCategory;
import com.simlect.entity.vo.ResponseVO;
import com.simlect.biz.SysCategoryService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@RestController("sysCategoryController")
@RequestMapping("/admin/sysCategory")
public class SysCategoryController extends com.simlect.controller.admin.ABaseController{

	@Resource
	private SysCategoryService sysCategoryService;

	@PostMapping("/loadCategory")
	public ResponseVO loadCategory(Boolean queryProperty){
		SysCategoryQuery query = new SysCategoryQuery();
		if (queryProperty!=null && queryProperty){
			query.setOrderBy("s.sort asc");
		}
		query.setPropertyQuery(queryProperty);
		query.setParent(true);
		return getSuccessResponseVO(sysCategoryService.findListByParam(query));
	}

	@PostMapping("/saveCategory")
	public ResponseVO saveCategory(SysCategory bean) {
		sysCategoryService.saveCategory(bean);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/addBatch")
	public ResponseVO addBatch(@RequestBody List<SysCategory> listBean) {
		sysCategoryService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/addOrUpdateBatch")
	public ResponseVO addOrUpdateBatch(@RequestBody List<SysCategory> listBean) {
		sysCategoryService.addOrUpdateBatch(listBean);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/getSysCategoryByCategoryId")
	public ResponseVO getSysCategoryByCategoryId(String categoryId) {
		return getSuccessResponseVO(sysCategoryService.getSysCategoryByCategoryId(categoryId));
	}

	@PostMapping("/updateSysCategoryByCategoryId")
	public ResponseVO updateSysCategoryByCategoryId(SysCategory bean,String categoryId) {
		sysCategoryService.updateSysCategoryByCategoryId(bean,categoryId);
		return getSuccessResponseVO(null);
	}

@AdminSensitiveConfirm
    	@PostMapping("/delCategory")
	public ResponseVO delCategory(SysCategory bean) {
		sysCategoryService.deleteSysCategory(bean);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/changeCategorySort")
	public ResponseVO changeCategorySort(String categoryIds) {
		sysCategoryService.changeCategorySort(categoryIds);
		return getSuccessResponseVO(null);
	}

	@PostMapping("/saveProductProperty")
	public ResponseVO saveProductProperty(SysProductProperty bean) {
		sysCategoryService.saveProductProperty(bean);
		return getSuccessResponseVO(null);
	}

@AdminSensitiveConfirm
    	@PostMapping("/delProductProperty")
	public ResponseVO delProductProperty(SysProductProperty bean) {
		sysCategoryService.delProductProperty(bean);
		return getSuccessResponseVO(null);
	}
}
