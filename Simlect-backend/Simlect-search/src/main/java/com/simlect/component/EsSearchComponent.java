package com.simlect.component;

import com.simlect.api.support.ProductFeignSupport;
import com.simlect.api.vo.ProductSearchIndexVO;
import com.simlect.api.dto.ProductInfoDTO;
import com.simlect.entity.enums.PageSize;
import com.simlect.api.enums.ProductStatusEnum;
import com.simlect.entity.vo.PaginationResultVO;
import com.simlect.utils.StringTools;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component("esSearchComponent")
@Slf4j
public class EsSearchComponent {

    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    @Resource
    private ProductFeignSupport productFeignSupport;

    @PostConstruct
    public void createIndexWithIK() {
        try {
            IndexOperations indexOperations = elasticsearchOperations.indexOps(ProductInfoDTO.class);
            if (indexOperations.exists()) {
                log.info("索引已存在，跳过创建");
                return;
            }
            Boolean success = indexOperations.createWithMapping();
            if (Boolean.TRUE.equals(success)) {
                log.info("索引创建成功（IK 分词）");
            } else {
                log.error("创建索引失败：ES 返回 false。请确认已安装 analysis-ik（镜像 simlect-elasticsearch:9.2.1-ik）");
            }
        } catch (Exception e) {
            log.error("创建索引失败（常见原因：ES 未安装 IK 插件 ik_max_word）。"
                    + "请重建中间件 ES：docker compose -f deploy/docker-compose.middleware.yml build elasticsearch --no-cache && up -d elasticsearch", e);
        }
    }

    // 保存索引
    public void saveIndex(String productId){
        ProductSearchIndexVO productInfo = productFeignSupport.getSearchIndex(productId);
        if (productInfo == null) {
            ProductInfoDTO deleteDto = new ProductInfoDTO();
            deleteDto.setProductId(productId);
            try {
                elasticsearchOperations.delete(deleteDto);
            } catch (Exception e) {
                log.warn("删除不存在商品的ES索引跳过 productId={}", productId);
            }
            return;
        }
        ProductInfoDTO productInfoDTO = new ProductInfoDTO();
        BeanUtils.copyProperties(productInfo, productInfoDTO);
        if (ProductStatusEnum.ON_SALE.getStatus().equals(productInfo.getStatus())){
            elasticsearchOperations.save(productInfoDTO);
        }else {
            elasticsearchOperations.delete(productInfoDTO);
        }
    }
    // 查询索引
    public PaginationResultVO<ProductInfoDTO> searchProducts(@NotEmpty String keyWords,
                                                             BigDecimal priceFrom,
                                                             BigDecimal priceTo,
                                                             String sortType,
                                                             String sortField,
                                                             Integer pageNo){
        try {
            // 参数处理
            pageNo = pageNo == null ? 1 : pageNo;
            //es 分页从0开始
            pageNo = pageNo - 1;
            int pageSize = PageSize.SIZE15.getSize();
            // 构建查询条件
            Criteria criteria = new Criteria();
            Boolean hasCondition = false;

            // 对于商品名
            // 如果productName少于等于2个字，精准匹配
            if (!StringTools.isEmpty(keyWords)) {
                Criteria productNameCriteria;
                if (keyWords.length() <= 2){
                    productNameCriteria = new Criteria("productName").contains(keyWords);
                    productNameCriteria.or(new Criteria("productName").matches(keyWords));
                    productNameCriteria.or(new Criteria("productName").expression("*" + keyWords + "*"));
                }else {
                    productNameCriteria = new Criteria("productName").contains(keyWords);
                }
                criteria.and(productNameCriteria);
                hasCondition = true;
            }

            // 对于价格（相交语义：商品价格区间与查询区间有交集才命中）
            if (priceFrom != null || priceTo != null){
                if (priceFrom != null && priceTo != null) {
                    // 商品 maxPrice >= 查询下限 且 商品 minPrice <= 查询上限
                    criteria.and(new Criteria("maxPrice").greaterThanEqual(priceFrom))
                            .and(new Criteria("minPrice").lessThanEqual(priceTo));
                } else if (priceFrom != null) {
                    // 只有最低价格：商品价格上限不低于查询下限
                    criteria.and(new Criteria("maxPrice").greaterThanEqual(priceFrom));
                } else {
                    // 只有最高价格：商品价格下限不高于查询上限
                    criteria.and(new Criteria("minPrice").lessThanEqual(priceTo));
                }
                hasCondition = true;
            }

            // 如果没有任何查询条件，使用 match_all 查询所有商品
            if (!hasCondition) {
                criteria = new Criteria("_index").exists();
            }

            // 对于分类
            // 如果sortField为空，则默认为综合分类
            if (sortField == null){
                sortField = "composite";
            }
            if (sortField.equals("composite")){
                sortField = "_score";
            }else if (sortField.equals("price")){
                sortField = "minPrice";
            }else if (sortField.equals("sale")){
                sortField = "totalSale";
            }

            // 对于排序
            // 如果sortType为空，则默认为降序
            Sort.Direction sortDirection = Sort.Direction.DESC;
            if (Objects.equals(sortType, "asc")){
                sortDirection = Sort.Direction.ASC;
            }

            Sort sort = Sort.by(sortDirection, sortField);
            // 分页
            Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
            // 执行查询
            CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);
            criteriaQuery.setPageable(pageable);
            SearchHits<ProductInfoDTO> searchHits = elasticsearchOperations.search(criteriaQuery, ProductInfoDTO.class);
            // 处理结果
            List<ProductInfoDTO> productInfoDTOList = searchHits.getSearchHits().stream().map(SearchHit::getContent).collect(Collectors.toList());
            long totalHits = searchHits.getTotalHits();
            int totalPage = (int) Math.ceil((double) totalHits / pageSize);
            return new PaginationResultVO<>((int) totalHits, pageSize, pageNo + 1, totalPage, productInfoDTOList);

        }catch (Exception e){
            log.error("搜索失败", e);
            throw new com.simlect.exception.BusinessException("搜索服务暂时不可用，请稍后重试");
        }
    }
}
