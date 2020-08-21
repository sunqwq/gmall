package com.atguigu.gmall.search.listener;


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
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

//消息队列  消费者
/**
 * 消费者确认机制
 */
@Component
public class ItemListener {
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GoodsRepository goodsRepository;



    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "PMS-SAVE-QUEUE",durable = "true"),
            exchange = @Exchange(value = "PMS-ITEM-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"item.insert","item.update"}
    ))
    public void listener1(Long squId) {
        //根据squId获取spu信息
        ResponseVo<SpuEntity> spuEntityResponseVo = pmsClient.querySpuById(squId);
        SpuEntity spuEntity = spuEntityResponseVo.getData();
        //非空验证
        if (spuEntity == null) {
            return;
        }

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

    }

}
