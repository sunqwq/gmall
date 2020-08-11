package com.atguigu.gmall.search.pojo;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import lombok.Data;

import java.util.List;

/**
 * 响应的数据模型
 */
@Data
public class SearchResponseVo {
    // 封装品牌过滤条件
    private List<BrandEntity> brands;
    // 封装分类过滤条件
    private List<CategoryEntity> categories;
    // 封装规格过滤条件：[{attrId: 8, attrName: "内存", attrValues: ["8G", "12G"]}, {attrId: 9, attrName: "机身存储", attrValues: ["128G", "256G"]}]
    private List<SearchResponseAttrVo> attrs;

    // 分页(当前页,每页多少条,总记录数,总页数=总记录数 / 每页多少条)
    private Integer pageNum;
    private Integer pageSize;
    private Long total;

    // 当前页数据
    private List<Goods> goodsList;
}
