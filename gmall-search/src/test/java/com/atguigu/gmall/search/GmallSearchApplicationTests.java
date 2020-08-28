package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.api.GmallWmsApi;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**  1. ES原生 客户端: TransportClient(即将废除)    RestClient
 *   2. spring整合的 Spring Data Elasticsearch
 *      ==>ElasticsearchTemplate是TransportClient客户端  (不怎么用)
 *      ==> ElasticsearchRestTemplate是RestHighLevel客户端 => 用于创建索引,映射,文档查询
 *   3. Repository
 *
 * 所以常用两种客户端: ElasticsearchRestTemplate  和 Repository
 *
 * ElasticsearchRestTemplate：基于High level rest client
 * 			createIndex(User.class)
 * 			putMapping(User.class)
 * ElasticsearchRepository：CRUD 分页排序
 * 	        save/saveAll
 * 			deleteById(1l)
 * 			findById()
 */
@SpringBootTest
class GmallSearchApplicationTests {
    // ElasticsearchTemplate是TransportClient客户端
    // ElasticsearchRestTemplate是RestHighLevel客户端

    @Autowired
    ElasticsearchRestTemplate elasticsearchRestTemplate;
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;


    /**
     * 由于数据导入只需导入一次，
     * 这里就写一个测试用例。后续索引库和数据库的数据同步，通过程序自身来维护。
     */
    @Test
    void contextLoads() {
//        // 创建索引
//        elasticsearchRestTemplate.createIndex(Goods.class);
//        // 创建映射
//        elasticsearchRestTemplate.putMapping(Goods.class);

        Integer pageNum = 1;
        Integer pageSize = 100;

       do {
           //查询条件
           PageParamVo pageParamVo = new PageParamVo();
           pageParamVo.setPageNum(pageNum);
           pageParamVo.setPageSize(pageSize);
           //查询spu
           ResponseVo<List<SpuEntity>> spuResponseVo = this.pmsClient.querySpuByPageJson(pageParamVo);
           List<SpuEntity> spuEntities = spuResponseVo.getData();
            //非空验证
           if (CollectionUtils.isEmpty(spuEntities)) {
               return;
           }

           //根据spuid查询spu下的每个sku
           spuEntities.forEach(spuEntity -> {
               ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsClient.querySkuBySpuId(spuEntity.getId());
               List<SkuEntity> skuEntities = skuResponseVo.getData();
               //非空验证
               if (!CollectionUtils.isEmpty(skuEntities)) {
                   List<Goods> goodLists = skuEntities.stream().map(skuEntity -> {
                       Goods goods = new Goods();
                       goods.setSkuId(skuEntity.getId());
                       goods.setImage(skuEntity.getDefaultImage());
                       goods.setPrice(skuEntity.getPrice().doubleValue());
                       goods.setTitle(skuEntity.getTitle());
                       goods.setSubTitle(skuEntity.getSubtitle());
                       goods.setCreateTime(spuEntity.getCreateTime());

                       //根据品牌id查询并赋值品牌
                       ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
                       BrandEntity brandEntitis = brandEntityResponseVo.getData();
                       if (brandEntitis != null) {
                           goods.setBrandId(brandEntitis.getId());
                           goods.setBrandName(brandEntitis.getName());
                           goods.setLogo(brandEntitis.getLogo());
                       }

                       //根据分类id查询并赋值分类
                       ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsClient.queryCategoryById(skuEntity.getCatagoryId());
                       CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
                       if (categoryEntity != null) {
                           goods.setCategoryId(categoryEntity.getId());
                           goods.setCategoryName(categoryEntity.getName());
                       }

                       //根据skuid查询并赋值销量 库存
                       ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkuBySkuId(skuEntity.getId());
                       List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
                       if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                           // 获取里面的销量 进行相加
                           goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a,b)->(a+b)).get().intValue());
                           goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                       }

                       //创建setSearchAttrs集合接收
                       List<SearchAttrValue> searchAttrValues = new ArrayList<>();
                       //根据skuid查询并赋值spu和sku的规格参数 ==> 添加到集合
                       ResponseVo<List<SkuAttrValueEntity>> skuAttrResponseVo = this.pmsClient.querySearchAttrValueBySkuId(skuEntity.getId());
                       List<SkuAttrValueEntity> skuAttrValueEntitis = skuAttrResponseVo.getData();
                       if (!CollectionUtils.isEmpty(skuAttrValueEntitis)) {
                           searchAttrValues.addAll(skuAttrValueEntitis.stream().map(skuAttrValueEntity -> {
                               SearchAttrValue searchAttrValue = new SearchAttrValue();
                               searchAttrValue.setAttrId(skuAttrValueEntity.getAttrId());
                               searchAttrValue.setAttrName(skuAttrValueEntity.getAttrName());
                               searchAttrValue.setAttrValue(skuAttrValueEntity.getAttrValue());
                               return searchAttrValue;
                           }).collect(Collectors.toList()));
                       }

                       ResponseVo<List<SpuAttrValueEntity>> spuAttrResponseVo = this.pmsClient.querySearchAttrValueBySpuId(spuEntity.getId());
                       List<SpuAttrValueEntity> spuAttrValueEntities = spuAttrResponseVo.getData();
                       if (!CollectionUtils.isEmpty(spuAttrValueEntities)) {
                           searchAttrValues.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                               SearchAttrValue searchAttrValue = new SearchAttrValue();
                               searchAttrValue.setAttrId(spuAttrValueEntity.getAttrId());
                               searchAttrValue.setAttrName(spuAttrValueEntity.getAttrName());
                               searchAttrValue.setAttrValue(spuAttrValueEntity.getAttrValue());
                               return searchAttrValue;
                           }).collect(Collectors.toList()));
                       }

                       goods.setSearchAttrs(searchAttrValues);

                       return goods;
                   }).collect(Collectors.toList());
                   this.goodsRepository.saveAll(goodLists);
               }

           });

           pageSize = spuEntities.size();
           pageNum++;
       }while (pageSize == 100);


    }

}
