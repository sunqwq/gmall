package com.atguigu.gmall.search.repository;

import com.atguigu.gmall.search.pojo.Goods;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

//继承即可 ,注意泛型

/**
 * 一种客户端: ElasticsearchRepository：CRUD 分页排序
 * * 	        save/saveAll  增/改
 * * 			deleteById(1l)
 * * 			findById()
 *
 * Spring Data 的另一个强大功能，是根据方法名称自动实现功能。
 * 比如：你的方法名叫做：findByTitle，那么它就知道你是根据title查询，然后自动帮你完成，无需写实现类。
 *
 * 也可以手动写方法
 */

public interface GoodsRepository extends ElasticsearchRepository<Goods, Long> {
    //方式一:
    //List<Goods> findByPriceBetween(double price1 , double price2);

    //方式二:
    /*@Query("{\n" +
            "    \"range\": {\n" +
            "      \"age\": {\n" +
            "        \"gte\": \"?0\",\n" +
            "        \"lte\": \"?1\"\n" +
            "      }\n" +
            "    }\n" +
            "  }")
    List<Goods> findByQuery(Integer age1, Integer age2);*/
}
