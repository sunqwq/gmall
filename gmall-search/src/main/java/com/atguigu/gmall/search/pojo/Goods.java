package com.atguigu.gmall.search.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

/**
 *Spring Data通过注解来声明字段的映射属性，有下面的三个注解：
 *
 *  `@Document` 作用在类，标记实体类为文档对象，一般有四个属性
 *          - indexName：对应索引库名称
 *          - type：对应在索引库中的类型  数据表
 *          - shards：分片数量，默认5
 *          - replicas：副本数量，默认1
 * `@Id` 作用在成员变量，标记一个字段作为id主键
 * `@Field` 作用在成员变量，标记为文档的字段，并指定字段映射属性：
 *          - type：字段类型，取值是枚举：FieldType
 *          - index：是否索引，布尔类型，默认是true
 *          - store：是否存储，布尔类型，默认是false
 *          - analyzer：分词器名称：ik_max_word
 *
 *    ElasticsearchTemplate是TransportClient客户端
 *    ElasticsearchRestTemplate是RestHighLevel客户端
 *
 *    每一个goods对应的是一个sku
 */
@Data
@Document(indexName = "goods", type = "info", shards = 3, replicas = 2)
public class Goods {

    // 商品详情列表中需要的字段(Keyword 不分词)
    @Id
    private Long skuId;
    @Field(type = FieldType.Keyword, index = false)
    private String image;
    @Field(type = FieldType.Double)
    private Double price;
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;
    //副标题
    @Field(type = FieldType.Keyword, index = false)
    private String subTitle;

    //排序过滤字段
    @Field(type = FieldType.Integer)
    private Integer sales;  //销量
    @Field(type = FieldType.Date)
    private Date createTime;  //新品排序
    @Field(type = FieldType.Boolean)
    private Boolean store = false;   //是否有货

    //品牌的聚合
    @Field(type = FieldType.Long)
    private Long brandId;   //品牌id
    @Field(type = FieldType.Keyword)
    private String brandName;  //品牌名称
    @Field(type = FieldType.Keyword)
    private String logo;     //品牌logo

    //分类
    @Field(type = FieldType.Long)
    private Long categoryId;    //分类id
    @Field(type = FieldType.Keyword)
    private String categoryName;   //分类名称

    //搜索的规格参数(Nested 代表嵌套类型  防止数据扁平化)
    @Field(type = FieldType.Nested)
    private List<SearchAttrValue> searchAttrs;


}