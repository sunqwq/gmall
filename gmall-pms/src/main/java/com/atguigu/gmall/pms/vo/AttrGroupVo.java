package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import lombok.Data;

import java.util.List;

/**
 *  为了 查询分类下的组及规格参数
 *  扩展vo
 */
@Data
public class AttrGroupVo extends AttrGroupEntity {
    //属性表 集合
    private List<AttrEntity> attrEntities;

}
