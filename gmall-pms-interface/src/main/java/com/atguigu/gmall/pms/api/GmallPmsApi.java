package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 属于pms
 * 专门提供api接口(feign)
 * 由调用者继承
 */

public interface GmallPmsApi {

    /**1.数据导入es 第一个接口
     * 查询数据导入es
     */
    @PostMapping("/pms/spu/json")
    public ResponseVo<List<SpuEntity>> querySpuByPageJson(@RequestBody PageParamVo paramVo);

    /**2.数据导入es 第二个接口
     * 查询spu下的所有sku信息
     */
    @GetMapping("pms/sku/spu/{spuId}")
    public ResponseVo<List<SkuEntity>> querySkuBySpuId(@PathVariable("spuId") Long spuId );

    /**3.数据导入es 第四个接口            商品详情页需要的数据接口：2
     * 根据品牌id查询品牌信息
     */
    @GetMapping("pms/brand/{id}")
    public ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);

    /**4.数据导入es 第五个接口
     * 根据分类id查询商品分类
     */
    @GetMapping("pms/category/{id}")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);


    /**5.数据导入es 第六个接口 1
     *根据spuId查询检索属性及值
     */
    @GetMapping("pms/spuattrvalue/search/{spuId}")
    public ResponseVo<List<SpuAttrValueEntity>> querySearchAttrValueBySpuId(@PathVariable("spuId") Long spuId);

    /**
     * 6.数据导入es 第六个接口 2
     * 根据skuId查询检索属性及值
     */
    @GetMapping("pms/skuattrvalue/search/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySearchAttrValueBySkuId(@PathVariable("skuId") Long skuId);

    /**   商品详情页需要的数据接口：3
     * 根据spuid获取spu信息
     */
    @GetMapping("pms/spu/{id}")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);

    /**
     * 一级分类
     */
    @GetMapping("pms/category/parent/{parentId}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesByPid(@PathVariable("parentId") Long pid);


    /**
     * 根据一级分类id查询二级分类 包含三级分类
     */
    @GetMapping("pms/category/cates/{pid}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesWithSubByPid(@PathVariable("pid") Long pid);

    /*  商品详情页需要的数据接口：4
    根据skuId查询sku信息
     */
    @GetMapping("pms/sku/{id}")
    public ResponseVo<SkuEntity> querySkuById(@PathVariable("id") Long id);

    /** 商品详情页需要的数据接口: 1
    根据三级分类的id查询一二三级分类的集合
     */
    @GetMapping("pms/category/all/{cid}")
    public ResponseVo<List<CategoryEntity>> query123categoriesByCid(@PathVariable("cid") Long cid);

    /**  商品详情页需要的数据接口：5
     * 根据skuId查询skuImage信息
     */
    @GetMapping("pms/skuimages/sku/{skuId}")
    public ResponseVo<List<SkuImagesEntity>> queryImagesByskuId(@PathVariable("skuId") Long skuId);

    /** 商品详情页需要的数据接口：8
     * 根据spuId查询spu下所有销售属性的可选值pms_sku_attr_value(颜色,内存,存储)
     */
    @GetMapping("pms/skuattrvalue/spu/{spuId}")
    public ResponseVo<List<SaleAttrValueVo>> queryAllSaleAttrValueBySpuId(@PathVariable("spuId") Long spuId);

    /** 商品详情页需要的数据接口：9
     * 根据skuId查询当前sku的销售属性集合(颜色,内存,存储)
     */
    @GetMapping("pms/skuattrvalue/sku/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySaleAttrValueBySkuId(@PathVariable("skuId") Long skuId);

    /** 商品详情页需要的数据接口：10
     * 根据spuId查询spu的描述信息(pms_spu_desc)
     */
    @GetMapping("pms/spudesc/{spuId}")
    public ResponseVo<SpuDescEntity> querySpuDescById(@PathVariable("spuId") Long spuId);

    /**  商品详情页需要的数据接口：11
     * 根据spuId查询spu下所有sku的销售属性attr_value与skuId之间的映射关系
     */
    @GetMapping("pms/skuattrvalue/sku/spu/{spuId}")
    public ResponseVo<String> querySaleAttrMappingSkuIdBySpuId(@PathVariable("spuId") Long spuId);

    /**  商品详情页需要的数据接口：12
     * 根据分类id结合spuId或者skuId查询组及组下规格参数与值
     */
    @GetMapping("pms/attrgroup/withattr/withvalue/{cid}")
    public ResponseVo<List<ItemGroupVo>> queryGroupWithAttrValue(@PathVariable("cid") Long cid,
                                                                 @RequestParam("spuId") Long spuId,
                                                                 @RequestParam("skuId") Long skuId);

}
