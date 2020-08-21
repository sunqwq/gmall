package com.atguigu.gmall.item.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import jdk.nashorn.internal.ir.IfNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItemService {
    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;


    public ItemVo loadData(Long skuId) {
        ItemVo itemVo = new ItemVo();
        // 1.根据skuId查询sku，设置sku相关信息
        ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(skuId);
        if (skuEntityResponseVo == null) {
            return null;
        }
        SkuEntity skuEntity = skuEntityResponseVo.getData();
        itemVo.setSkuId(skuEntity.getId());
        itemVo.setTitle(skuEntity.getTitle());
        itemVo.setSubTitle(skuEntity.getSubtitle());
        itemVo.setPrice(skuEntity.getPrice());
        itemVo.setWeight(skuEntity.getWeight());
        itemVo.setDefaultImage(skuEntity.getDefaultImage());
        // 2.根据sku中的categoryId查询一二三级分类
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.query123categoriesByCid(skuEntity.getCatagoryId());
        itemVo.setCategories(listResponseVo.getData());
        // 3.根据sku中的brandId查询品牌
        ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
        BrandEntity brandEntity = brandEntityResponseVo.getData();
        if (brandEntity != null) {
            itemVo.setBrandId(brandEntity.getId());
            itemVo.setBrandName(brandEntity.getName());
        }
        // 4.根据sku中的spuId查询spu
        ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
        SpuEntity spuEntity = spuEntityResponseVo.getData();
        if (spuEntity != null) {
            itemVo.setSpuId(spuEntity.getId());
            itemVo.setSpuName(spuEntity.getName());
        }
        // 5.根据skuId查询sku的图片信息
        ResponseVo<List<SkuImagesEntity>> imagesByskuId = this.pmsClient.queryImagesByskuId(skuId);
        itemVo.setImages(imagesByskuId.getData());
        // 6.根据skuId查询sku的营销信息
        ResponseVo<List<ItemSaleVo>> itemSaleResponseVo = this.smsClient.querysalesByskuId(skuId);
        itemVo.setSales(itemSaleResponseVo.getData());
        // 7.根据skuId查询库存信息
        ResponseVo<List<WareSkuEntity>> wareSkuBySkuId = this.wmsClient.queryWareSkuBySkuId(skuId);
        List<WareSkuEntity> wareSkuEntities = wareSkuBySkuId.getData();
        if (!CollectionUtils.isEmpty(wareSkuEntities)) {
            itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
        }

        // 8.根据sku中的spuId查询spu下所有sku的选择值集合
        ResponseVo<List<SaleAttrValueVo>> saleAttrValueVo = this.pmsClient.queryAllSaleAttrValueBySpuId(skuEntity.getSpuId());
        itemVo.setSaleAttrs(saleAttrValueVo.getData());
        // 9.根据skuId查询当前sku的销售属性 (需要转为map)
        ResponseVo<List<SkuAttrValueEntity>> saleAttrValueBySkuId = this.pmsClient.querySaleAttrValueBySkuId(skuId);
        List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrValueBySkuId.getData();
        if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
            Map<Long, String> collect = skuAttrValueEntities.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue));
            itemVo.setSaleAttr(collect);
        }
        // 10.根据sku中spuId查询spu下所有sku销售属性组合与skuId的映射关系
        ResponseVo<String> stringResponseVo = this.pmsClient.querySaleAttrMappingSkuIdBySpuId(skuEntity.getSpuId());
        itemVo.setSkuJson(stringResponseVo.getData());
        // 11.根据sku中spuId查询spu的描述信息（海报信息） (需要以逗号分开 转集合)
        ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
        SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
        if (spuDescEntity != null) {
            List<String> strings = Arrays.asList(StringUtils.split(spuDescEntity.getDecript(), ","));
            itemVo.setSpuImages(strings);
        }

        // 12.根据sku中的分类id、spuId以及skuId 查询组及组下的规格参数和值
        ResponseVo<List<ItemGroupVo>> queryGroupWithAttrValue = this.pmsClient.queryGroupWithAttrValue(skuEntity.getCatagoryId(), skuEntity.getSpuId(), skuId);
        itemVo.setGroups(queryGroupWithAttrValue.getData());

        System.out.println("itemVo = " + itemVo);
        return itemVo;
    }
}
