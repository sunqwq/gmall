package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.vo.AttrGroupVo;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.AttrGroupMapper;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import org.springframework.util.CollectionUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupMapper, AttrGroupEntity> implements AttrGroupService {

    //引入arrt的mapper
    @Autowired
    private AttrMapper attrMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SpuAttrValueMapper spuAttrValueMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<AttrGroupEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageResultVo(page);
    }


    /**
     * 2.查询分类下的组及规格参数
     */
    @Override
    public List<AttrGroupVo> queryGroupsWithAttrsByCid3(Long catId) {
        // 根据catId 查询属性分组表
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", catId));
        if (CollectionUtils.isEmpty(attrGroupEntities)) {
            return null;
        }

        return attrGroupEntities.stream().map(attrGroupEntity -> {
            AttrGroupVo attrGroupVo = new AttrGroupVo();
            BeanUtils.copyProperties(attrGroupEntity, attrGroupVo);

            // 根据 组id查询 属性表
            List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()).eq("type", 1));
            attrGroupVo.setAttrEntities(attrEntities);
            return attrGroupVo;
        }).collect(Collectors.toList());

    }

    /**
     * 根据分类id结合spuId或者skuId查询组及组下规格参数与值
     */
    @Override
    public List<ItemGroupVo> queryGroupWithAttrValue(Long cid, Long spuId, Long skuId) {
        //1.根据cid查询所有的组
        List<AttrGroupEntity> attrGroupEntities = this.baseMapper.selectList(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
        if (CollectionUtils.isEmpty(attrGroupEntities)) {
            return null;
        }
        //2.根据组id 查询组下规格参数与值
        return attrGroupEntities.stream().map(attrGroupEntity -> {
            ItemGroupVo groupVo = new ItemGroupVo();
            //设置groupid,groupname,list<AttrValueVo> attrs
            groupVo.setGroupId(attrGroupEntity.getId());
            groupVo.setGroupName(attrGroupEntity.getName());
            //所有的规格参数
            List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()));
            if (CollectionUtils.isEmpty(attrEntities)) {
                return groupVo;
            }
            List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());

            /*新建封装attrs的集合    List<AttrValueVo> attrs*/
            List<AttrValueVo> attrValueVos = new ArrayList<>();
            //3.查询spu的规格参数及值
            List<SpuAttrValueEntity> spuAttrValueEntities = this.spuAttrValueMapper.selectList(new QueryWrapper<SpuAttrValueEntity>().in("attr_id", attrIds).eq("spu_id", spuId));
            if (!CollectionUtils.isEmpty(spuAttrValueEntities)) {
                //spuAttrValueEntity 转为AttrValueVo  封装为集合
                attrValueVos.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                    // 方式一
                    AttrValueVo spuattrValueVo = new AttrValueVo();

                    spuattrValueVo.setAttrId(spuAttrValueEntity.getAttrId());
                    spuattrValueVo.setAttrName(spuAttrValueEntity.getAttrName());
                    spuattrValueVo.setAttrValue(spuAttrValueEntity.getAttrValue());
                    return spuattrValueVo;
                }).collect(Collectors.toList()));
            }

            //4.查询sku的规格参数及值
            List<SkuAttrValueEntity> skuAttrValueEntities = this.skuAttrValueMapper.selectList(new QueryWrapper<SkuAttrValueEntity>().in("attr_id", attrIds).eq("sku_id", skuId));
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                //skuAttrValueEntity 转为AttrValueVo  封装为集合
                attrValueVos.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                    AttrValueVo skuattrValueVo = new AttrValueVo();
                    // 方式二
                    BeanUtils.copyProperties(skuAttrValueEntity, skuattrValueVo);

                    return skuattrValueVo;
                }).collect(Collectors.toList()));
            }

            groupVo.setAttrs(attrValueVos);
            return groupVo;
        }).collect(Collectors.toList());


    }

}