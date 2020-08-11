package com.atguigu.gmall.search.repository;

import com.atguigu.gmall.search.pojo.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

//继承即可 ,注意泛型

/**
 *  一种客户端: ElasticsearchRepository：CRUD 分页排序
 *  * 	        save/saveAll
 *  * 			deleteById(1l)
 *  * 			findById()
 */

public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {
}
