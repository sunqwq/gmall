package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 * 
 * @author fjy
 * @email 1159213392@qq.com
 * @date 2020-08-02 18:33:25
 */
@Mapper
public interface SkuAttrValueMapper extends BaseMapper<SkuAttrValueEntity> {

    // 1.根据spuId查询检索属性及值
    List<SkuAttrValueEntity> querySearchAttrValueBySkuId(Long skuId);

    /**
     * 根据spuId查询spu下所有sku的销售属性attr_value与skuId之间的映射关系
     * {'8G,128G,暗夜黑':100,'8G,128G,天空白':101}
     */
    List<Map<String, Object>> querySaleAttrMappingSkuIdBySpuId(Long spuId);
}
