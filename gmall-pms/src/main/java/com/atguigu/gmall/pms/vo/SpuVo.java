package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuEntity;
import lombok.Data;

import java.util.List;

@Data
public class SpuVo extends SpuEntity {

    private List<String> spuImages;
    // 对应SpuAttrValueEntity表
    private List<BaseAttrValueVo> baseAttrs;

    private List<SkuVo> skus;
}
