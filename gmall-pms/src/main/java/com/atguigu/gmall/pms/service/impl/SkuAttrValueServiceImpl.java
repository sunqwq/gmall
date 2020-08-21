package com.atguigu.gmall.pms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.springframework.util.CollectionUtils;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {

    @Autowired
    private SkuMapper skuMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    // 1.根据spuId查询检索属性及值
    @Override
    public List<SkuAttrValueEntity> querySearchAttrValueBySkuId(Long skuId) {
        return this.baseMapper.querySearchAttrValueBySkuId(skuId);
    }

    /**
     * 根据spuId查询spu下所有销售属性pms_sku_attr_value的可选值(颜色,内存,存储)
     */
    @Override
    public List<SaleAttrValueVo> queryAllSaleAttrValueBySpuId(Long spuId) {
        // 1.先根据spuId查询所有的sku
        List<SkuEntity> skuEntityList = this.skuMapper.selectList(new QueryWrapper<SkuEntity>().eq("spu_id", spuId));
        if (CollectionUtils.isEmpty(skuEntityList)) {
            return null;
        }
        // 2.获取sku的id组成新的id集合
        List<Long> skuIds = skuEntityList.stream().map(SkuEntity::getId).collect(Collectors.toList());
        // 3.根据skuids查询所有的销售属性
        List<SkuAttrValueEntity> skuAttrValueEntities = this.list(new QueryWrapper<SkuAttrValueEntity>().in("sku_id", skuIds));
        if (CollectionUtils.isEmpty(skuAttrValueEntities)) {
            return null;
        }
        // 4.根据attrid分组
        Map<Long, List<SkuAttrValueEntity>> map = skuAttrValueEntities.stream().collect(Collectors.groupingBy(SkuAttrValueEntity::getAttrId));

        List<SaleAttrValueVo> saleAttrValueVos = new ArrayList<>();
        // 5.把map的每一个kv变成SaleAttrValueVo {attrId:4,attrName:内存,attrValues:[8G,12G]}
        map.forEach((attrId,attrValueEntities) -> {
            SaleAttrValueVo attrValueVo = new SaleAttrValueVo();
            //设置attrId
            attrValueVo.setAttrId(attrId);
            if (!CollectionUtils.isEmpty(attrValueEntities)) {
                //设置attrName
                attrValueVo.setAttrName(attrValueEntities.get(0).getAttrName());
                //设置attrValues   将value转为set集合  去重
                Set<String> collect = attrValueEntities.stream().map(SkuAttrValueEntity::getAttrValue).collect(Collectors.toSet());
                attrValueVo.setAttrValues(collect);
            }
            saleAttrValueVos.add(attrValueVo);
        });

        return saleAttrValueVos;
    }

    /**
     * 根据skuId查询当前sku的销售属性集合(颜色,内存,存储)
     */
    @Override
    public List<SkuAttrValueEntity> querySaleAttrValueBySkuId(Long skuId) {
        List<SkuAttrValueEntity> skuAttrValueEntities = this.baseMapper.selectList(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId));
        return skuAttrValueEntities;
    }

    /**
     * 根据spuId查询spu下所有sku的销售属性attr_value与skuId之间的映射关系
     * {'8G,128G,暗夜黑':100,'8G,128G,天空白':101}
     */
    @Override
    public String querySaleAttrMappingSkuIdBySpuId(Long spuId) {
        //此时获得类型是 [{1:黑色,8G,128G},{2:256G,8G,白色}]
        List<Map<String,Object>> maps = this.baseMapper.querySaleAttrMappingSkuIdBySpuId(spuId);
        //转换成新的map {'8G,128G,暗夜黑':100,'8G,128G,天空白':101}
        Map<String, Long> skumap = maps.stream().collect(Collectors.toMap(map -> map.get("attr_values").toString(), map -> (Long) map.get("sku_id")));
        //序列化
        return JSON.toJSONString(skumap);
    }

}