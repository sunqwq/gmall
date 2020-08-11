package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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

    /**3.数据导入es 第四个接口
     * 根据品牌id查询品牌
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

    /**6.数据导入es 第六个接口 2
     * 根据skuId查询检索属性及值
     */
    @GetMapping("pms/skuattrvalue/search/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySearchAttrValueBySkuId(@PathVariable("skuId") Long skuId);
}
