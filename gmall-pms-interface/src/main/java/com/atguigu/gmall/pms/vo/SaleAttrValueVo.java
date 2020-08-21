package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class SaleAttrValueVo {

    private Long attrId;
    private String attrName;
    //使用set 为了去重
    private Set<String> attrValues;

}
