package com.atguigu.gmall.search;

import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.repository.GoodsRepository;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

public class testNativeSearch {
    @Autowired
    private GoodsRepository repository;

    /**
     * 测试  NativeSearchQueryBuilder
     */
    @Test
    public void NativeSearch() {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //搜索工具类 QueryBuilders
        queryBuilder.withQuery(QueryBuilders.rangeQuery("price").gte("200").lte("3000"));
        //排序工具类 SortBuilders
        queryBuilder.withSort(SortBuilders.fieldSort("price").order(SortOrder.DESC));
        //分页  (页码从0开始)
        queryBuilder.withPageable(PageRequest.of(1, 2));
        //聚合工具类AggregationBuilders
        queryBuilder.addAggregation(AggregationBuilders.terms("passwordAgg").field("password"));
        AggregatedPage<Goods> search = (AggregatedPage) this.repository.search(queryBuilder.build());
        System.out.println(" 总页数: " + search.getTotalPages());
        System.out.println(" 总记录数: " + search.getTotalElements());
        System.out.println(search.getContent());


    }
}
