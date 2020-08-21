package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 *
 * @author fjy
 * @email 1159213392@qq.com
 * @date 2020-08-02 18:33:25
 */
public interface SkuAttrValueService extends IService<SkuAttrValueEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    // 1.根据spuId查询检索属性及值
    List<SkuAttrValueEntity> querySearchAttrValueBySkuId(Long skuId);

    /**
     * 根据spuId查询spu下所有销售属性的可选值(颜色,内存,存储)
     */
    List<SaleAttrValueVo> queryAllSaleAttrValueBySpuId(Long spuId);

    /**
     * 根据skuId查询当前sku的销售属性集合(颜色,内存,存储)
     */
    List<SkuAttrValueEntity> querySaleAttrValueBySkuId(Long skuId);

    /**
     * 根据spuId查询spu下所有sku的销售属性attr_value与skuId之间的映射关系
     * {'8G,128G,暗夜黑':100,'8G,128G,天空白':101}
     */
    String querySaleAttrMappingSkuIdBySpuId(Long spuId);

}

