package com.atguigu.gmall.search.service;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**搜索的业务逻辑
 *  SearchParamVo 是前端传过来的条件
 *
 * Document ==>Row 行
 * Field  ==>Columns 列
 */

@Service
public class SearchService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;
    //json转换工具
    private static ObjectMapper mapper = new ObjectMapper();

    public SearchResponseVo search(SearchParamVo paramVo) {
        try {
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.indices("goods");
            searchRequest.source(buildDsl(paramVo));
            SearchResponse response = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            System.out.println("响应数据response = " + response.toString());

            SearchResponseVo responseVo = this.parseResult(response);
            // 从paramVo获取分页信息赋值SearchResponseVo
            responseVo.setPageNum(paramVo.getPageNum());
            responseVo.setPageSize(paramVo.getPageSize());
            return responseVo;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 构建查询DSL语句
     *
     */
    private SearchSourceBuilder buildDsl(SearchParamVo paramVo) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 检索条件
        String keyword = paramVo.getKeyword();
        if (StringUtils.isEmpty(keyword)) {
            // 打广告，TODO
            return null;
        }
        // 1. 构建查询条件（bool查询）
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 1.1. 匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyword).operator(Operator.AND));
        // 1.2 过滤
        // 1.2.1. 品牌过滤(terms)
        List<Long> brandId = paramVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",brandId));
        }
        // 1.2.2. 分类过滤(terms)
        List<Long> cid = paramVo.getCid();
        if (!CollectionUtils.isEmpty(cid)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId",cid));
        }
        // 1.2.3. 价格区间过滤
        Double priceFrom = paramVo.getPriceFrom();
        Double priceTo = paramVo.getPriceTo();
        if (priceFrom != null || priceTo != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            if (priceFrom != null) {
                rangeQuery.gte(priceFrom);
            }
            if (priceTo != null) {
                rangeQuery.lte(priceTo);
            }
            boolQueryBuilder.filter(rangeQuery);
        }
        // 1.2.4. 是否有货
        Boolean store = paramVo.getStore();
        if (store != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("store", store));
        }
        // 1.2.5. 规格参数的过滤 props=5:高通-麒麟&props=6:8G-12G
        List<String> props = paramVo.getProps();
        if (!CollectionUtils.isEmpty(props)) {
            props.forEach(prop -> {   // 每一个prop:  6:8G-12G
                //先以：分割获取规格参数id 以及8G-12G
                String[] attrs = StringUtils.split(prop, ":");
                if (attrs != null && attrs.length == 2) {
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    //注意是 term
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId", attrs[0]));
                    // 再以-分割获取规格参数值数组
                    String[] attrValues = StringUtils.split(attrs[1], "-");
                    if (attrValues != null && attrValues.length > 0) {
                        //注意是 terms
                        boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue",attrValues));
                    }

                    // 注意是嵌套查询  防止数据扁平化
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs", boolQuery, ScoreMode.None));

                }
            });
        }
        sourceBuilder.query(boolQueryBuilder);

        // 2. 构建排序 0-默认，得分降序；1-按价格升序；2-按价格降序；3-按创建时间降序；4-按销量降序
        Integer sort = paramVo.getSort();
        if (sort != null) {
            switch (sort) {
                case 1: sourceBuilder.sort("price", SortOrder.ASC); break;
                case 2: sourceBuilder.sort("price", SortOrder.DESC); break;
                case 3: sourceBuilder.sort("createTime", SortOrder.DESC); break;
                case 4: sourceBuilder.sort("sales", SortOrder.DESC); break;
                default:sourceBuilder.sort("_score", SortOrder.DESC); break;
            }
        }



        // 3. 构建分页 (页码从0开始)
        Integer pageNum = paramVo.getPageNum();
        Integer pageSize = paramVo.getPageSize();
        sourceBuilder.from((pageNum -1)* pageSize);
        sourceBuilder.size(pageSize);

        // 4. 构建高亮
        sourceBuilder.highlighter(new HighlightBuilder()
                .field("title")
                .preTags("<font style='color:red'>")
                .postTags("</font>"));

        // 5. 构建聚合  (AggregationBuilders 工具类  terms 划分桶的方式，这里是根据词条划分 ; field 划分桶的字段)
        // 5.1. 构建品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("brandLogoAgg").field("logo")));
        // 5.2. 构建分类聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));
        // 5.3. 构建规格参数的嵌套聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))));

        // 6.指定包含的字段  (构建结果集过滤)
        sourceBuilder.fetchSource(new String[]{"skuId", "title", "price", "image", "subTitle"}, null);
        System.out.println("sourceBuilder:  " +sourceBuilder.toString());

        return sourceBuilder;
    }



    private SearchResponseVo parseResult(SearchResponse response) {
        SearchResponseVo responseVo = new SearchResponseVo();

        // 总记录数
        SearchHits hits = response.getHits();
        responseVo.setTotal(hits.getTotalHits());

        //当前页数据
        SearchHit[] hitsHits = hits.getHits();
        if (hitsHits == null || hitsHits.length == 0) {
            return responseVo;

        }
        if (hitsHits != null) {
            List<Goods> goodsList = Stream.of(hitsHits).map(hit -> {
                try {
                    // 获取命中结果集中的_source
                    String source = hit.getSourceAsString();
                    // 把_source反序列化为Goods对象
                    Goods goods = mapper.readValue(source, Goods.class);
                    // 把_source中的普通的Title 替换成 高亮结果集中title
                    Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                    if (!CollectionUtils.isEmpty(highlightFields)) {
                        HighlightField highlight = highlightFields.get("title");
                        if (highlight != null) {
                            Text[] fragments = highlight.getFragments();
                            if (fragments != null && fragments.length > 0) {
                                String title = fragments[0].string();
                                goods.setTitle(title);
                            }
                        }
                    }
                    return goods;
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.toList());
            responseVo.setGoodsList(goodsList);
        }

        // 获取聚合
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();

        // 解析品牌id聚合获取品牌的过滤条件        ParsedLongTerms解析long类型的词条
        ParsedLongTerms brandIdAgg = (ParsedLongTerms) aggregationMap.get("brandIdAgg");
        if (brandIdAgg != null) {
            List<? extends Terms.Bucket> brandIdAggBuckets = brandIdAgg.getBuckets();
            if (!CollectionUtils.isEmpty(brandIdAggBuckets)) {
                List<BrandEntity> brndEntities = brandIdAggBuckets.stream().map(bucket -> {
                    BrandEntity brandEntity = new BrandEntity();
                    // 解析出桶中的品牌id
                    brandEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());

                    // 获取桶中的子聚合
                    Map<String, Aggregation> subAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
                    // 获取品牌名称的子聚合        ParsedStringTerms  解析string类型的词条
                    ParsedStringTerms brandNameAgg = (ParsedStringTerms) subAggregationMap.get("brandNameAgg");
                    if (brandNameAgg != null) {
                        List<? extends Terms.Bucket> brandNameAggBuckets = brandNameAgg.getBuckets();
                        if (!CollectionUtils.isEmpty(brandNameAggBuckets)) {
                            Terms.Bucket brandName = brandNameAggBuckets.get(0);
                            if (brandName != null) {
                                brandEntity.setName(brandName.getKeyAsString());
                            }
                        }
                    }

                    // 获取logo的子聚合
                    ParsedStringTerms brandLogoAgg = (ParsedStringTerms) subAggregationMap.get("brandLogoAgg");
                    if (brandLogoAgg != null) {
                        List<? extends Terms.Bucket> brandLogoAggBuckets = brandLogoAgg.getBuckets();
                        if (!CollectionUtils.isEmpty(brandLogoAggBuckets)) {
                            Terms.Bucket brandLogo = brandLogoAggBuckets.get(0);
                            if (brandLogo != null) {
                                brandEntity.setLogo(brandLogo.getKeyAsString());
                            }
                        }
                    }

                    return brandEntity;
                }).collect(Collectors.toList());
                responseVo.setBrands(brndEntities);
            }
        }



        // 解析分类id聚合获取分类的过滤条件
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggregationMap.get("categoryIdAgg");
        if (categoryIdAgg != null) {
            List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();
            if (!CollectionUtils.isEmpty(categoryIdAggBuckets)) {
                List<CategoryEntity> categoryEntitis = categoryIdAggBuckets.stream().map(bucket -> {
                    CategoryEntity categoryEntity = new CategoryEntity();
                    // 解析出桶中的分类id
                    categoryEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                    // 获取桶中的子聚合==>获取分类名称的子聚合(因为只有一个分类名称的子聚合,所以两步合二为一了)
                    ParsedStringTerms categoryNameAgg = (ParsedStringTerms) ((Terms.Bucket) bucket).getAggregations().get("categoryNameAgg");
                    if (categoryNameAgg != null) {
                        List<? extends Terms.Bucket> categoryNameAggBuckets = categoryNameAgg.getBuckets();
                        if (!CollectionUtils.isEmpty(categoryNameAggBuckets)) {
                            Terms.Bucket categoryName = categoryNameAggBuckets.get(0);
                            if (categoryName != null) {
                                categoryEntity.setName(categoryName.getKeyAsString());
                            }
                        }
                    }

                    return categoryEntity;
                }).collect(Collectors.toList());
                responseVo.setCategories(categoryEntitis);
            }
        }


        //获取规格参数的聚合
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrAgg.getAggregations().get("attrIdAgg");

        if (attrIdAgg != null) {
            List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
            if (!CollectionUtils.isEmpty(attrIdAggBuckets)) {
                List<SearchResponseAttrVo> searchResponseAttrVo = attrIdAggBuckets.stream().map(bucket -> {
                    SearchResponseAttrVo responseAttrVo = new SearchResponseAttrVo();
                    // 解析桶中的key获取规格参数的id
                    responseAttrVo.setAttrId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                    // 获取该参数所有的子聚合
                    Map<String, Aggregation> subAggregationMap  = ((Terms.Bucket) bucket).getAggregations().asMap();
                    // 获取规格参数名的子聚合
                    ParsedStringTerms attrNameAgg = (ParsedStringTerms) subAggregationMap.get("attrNameAgg");
                    if (attrNameAgg != null) {
                        List<? extends Terms.Bucket> attrNamebucket = attrNameAgg.getBuckets();
                        if (!CollectionUtils.isEmpty(attrNamebucket)) {
                            Terms.Bucket attrName = attrNamebucket.get(0);
                            if (attrName != null) {
                                responseAttrVo.setAttrName(attrName.getKeyAsString());
                            }
                        }
                    }

                    // 获取规格参数值的子聚合
                    ParsedStringTerms attrValueAgg = (ParsedStringTerms) subAggregationMap.get("attrValueAgg");
                    if (attrValueAgg != null) {
                        List<? extends Terms.Bucket> attrValueAggBuckets = attrValueAgg.getBuckets();
                        if (!CollectionUtils.isEmpty(attrValueAggBuckets)) {
                            //将key转为集合
                            responseAttrVo.setAttrValues(attrValueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList()));
                        }
                    }

                    return responseAttrVo;
                }).collect(Collectors.toList());
                responseVo.setFilters(searchResponseAttrVo);
            }
        }

        return responseVo;
    }

}
