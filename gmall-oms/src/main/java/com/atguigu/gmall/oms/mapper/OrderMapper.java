package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author fjy
 * @email 1159213392@qq.com
 * @date 2020-08-21 12:47:42
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
	
}
