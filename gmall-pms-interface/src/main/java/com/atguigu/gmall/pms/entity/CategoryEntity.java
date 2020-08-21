package com.atguigu.gmall.pms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 商品三级分类
 * 
 * @author fjy
 * @email 1159213392@qq.com
 * @date 2020-08-02 18:33:25
 */
@Data
@TableName("pms_category")
public class CategoryEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 分类id
	 */
	@TableId
	private Long id;
	/**
	 * 分类名称
	 */
	private String name;
	/**
	 * 父分类id
	 */
	private Long parentId;
	/**
	 * 是否显示[0-不显示，1显示]
	 */
	private Integer status;
	/**
	 * 排序
	 */
	private Integer sort;
	/**
	 * 图标地址
	 */
	private String icon;
	/**
	 * 计量单位
	 */
	private String unit;

	/**
	 * 增加的三级分类字段, 添加备注指明不存在,不然数据库没有会报错
	 *      exist是否为数据库表字段
	 *      默认 true 存在，false 不存在
	 */
	@TableField(exist = false)
	private List<CategoryEntity> subs;
}
