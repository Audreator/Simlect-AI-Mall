package com.simlect.entity.query;

import com.simlect.exception.BusinessException;

import java.util.regex.Pattern;

public class BaseParam {
	private SimplePage simplePage;
	private Integer pageNo;
	private Integer pageSize;
	private String orderBy;

	/**
	 * 排序参数结构白名单（防 SQL 注入）：
	 * 仅允许 [表别名.]列名 [ASC|DESC]，多排序用逗号分隔；唯一允许的表达式是内部代码使用的 COALESCE(col1, col2)。
	 * 禁止分号 / 括号嵌套（除 COALESCE）/ 注释 / 字符串字面量 / 函数注入。
	 */
	private static final String ORDER_BY_ITEM =
			"(COALESCE\\([a-z0-9_]+(\\s*,\\s*[a-z0-9_]+)*\\)|([a-z0-9_]+\\.)?[a-z0-9_]+)(\\s+(asc|desc))?";
	private static final Pattern ORDER_BY_SAFE = Pattern.compile(
			"(?i)^" + ORDER_BY_ITEM + "(\\s*,\\s*" + ORDER_BY_ITEM + ")*$");

	private static final int ORDER_BY_MAX_LENGTH = 120;

	public SimplePage getSimplePage() {
		return simplePage;
	}

	public void setSimplePage(SimplePage simplePage) {
		this.simplePage = simplePage;
	}

	public Integer getPageNo() {
		return pageNo;
	}

	public void setPageNo(Integer pageNo) {
		this.pageNo = pageNo;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * 排序字段校验：拒绝注入（非法字符/函数/子查询/注释等直接抛业务异常）。
	 * 该入口覆盖所有 HTTP 直接绑定 Query 的分页接口。
	 */
	public void setOrderBy(String orderBy){
		if (orderBy == null) {
			this.orderBy = null;
			return;
		}
		String trimmed = orderBy.trim();
		if (trimmed.length() > ORDER_BY_MAX_LENGTH || !ORDER_BY_SAFE.matcher(trimmed).matches()) {
			throw new BusinessException("非法排序参数");
		}
		this.orderBy = trimmed;
	}

	public String getOrderBy(){
		return this.orderBy;
	}
}
