package com.atguigu.gmall.oms.service;

import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.oms.entity.OrderEntity;

import java.util.Map;

/**
 * 订单
 *
 * @author fjy
 * @email 1159213392@qq.com
 * @date 2020-08-21 12:47:42
 */
public interface OrderService extends IService<OrderEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    /**
     * 提交订单处理步骤：
     * 		4.下单操作（新增订单表,订单详情表）
     */
    OrderEntity saveOrder(OrderSubmitVo submitVo, Long userId);
}

