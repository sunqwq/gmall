package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单 结算
 *
 * 结算提交订单处理步骤：
 * 		1.验证是否重复提交      =>orderToken; // 防止重复点击的唯一标识
 * 		2.校验商品的实时总价和页面总价是否一致
 * 		3.验库存并锁库存  ( 要具备原子性 )
 * 		4.下单操作（新增）
 * 		5.删除购物车中对应商品   (可以异步执行 ,但不能用springTask,因为现在在order服务器,删除操作在cart服务器,需要分布式异步==>MQ消息队列)
 *
 */
@Data
public class OrderSubmitVo {

    private String orderToken;  //防重，生成唯一标识
    private BigDecimal totalPrice;  // 验价 , 要一致
    private UserAddressEntity address;  // 收货地址
    private Integer payType;         // 支付方式
    private String deliveryCompany;   // 物流公司(配送方式)
    private List<OrderItemVo> items; // 送货清单
    private Integer bounds;  //   积分信息
    private BigDecimal postFee;  //邮费
}
