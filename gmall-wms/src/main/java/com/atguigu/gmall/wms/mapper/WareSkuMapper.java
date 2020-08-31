package com.atguigu.gmall.wms.mapper;

import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author fjy
 * @email 1159213392@qq.com
 * @date 2020-08-04 22:37:25
 */
@Mapper
public interface WareSkuMapper extends BaseMapper<WareSkuEntity> {
    // 验库存，查询库存
    public List<WareSkuEntity> check(@Param("skuId") Long skuId,@Param("count") Integer count);

    // 锁库存 更新库存  仓库id
    public int lock(@Param("id") Long id,@Param("count") Integer count);

    // 解锁 锁定成功的商品库存   仓库id
    public int unlock(@Param("id") Long id,@Param("count") Integer count);

    // 支付成功后 , 减库存
    public int minusStock(@Param("id") Long id,@Param("count") Integer count);
}
