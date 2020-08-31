package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.feign.GmallSmsClient;
import com.atguigu.gmall.oms.feign.GmallUmsClient;
import com.atguigu.gmall.oms.mapper.OrderItemMapper;
import com.atguigu.gmall.oms.service.OrderItemService;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {
    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<OrderEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<OrderEntity>()
        );

        return new PageResultVo(page);
    }

    /**
     * 提交订单处理步骤：
     * 		4.下单操作（新增订单表,订单详情表）
     * 	本地事务 ,创建失败 回滚
     */
    @Transactional
    @Override
    public OrderEntity saveOrder(OrderSubmitVo submitVo, Long userId) {
        List<OrderItemVo> items = submitVo.getItems();
        if (CollectionUtils.isEmpty(items)) {
            throw new OrderException("该订单没有选中商品!");
        }

        // 1.新增订单表
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setUserId(userId);
        orderEntity.setOrderSn(submitVo.getOrderToken());
        orderEntity.setCreateTime(new Date());
        // 根据用户id查询用户信息
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        orderEntity.setUsername(userEntity.getUsername());
        orderEntity.setTotalAmount(submitVo.getTotalPrice());
        orderEntity.setPayAmount(submitVo.getTotalPrice().add(submitVo.getPostFee()==null ? new BigDecimal(0) : submitVo.getPostFee()).subtract(new BigDecimal(submitVo.getBounds() / 100)));
        orderEntity.setFreightAmount(submitVo.getPostFee());
        //TODO 查询营销信息,计算优化金额
        orderEntity.setPromotionAmount(null);

        orderEntity.setIntegrationAmount(new BigDecimal(submitVo.getBounds() / 100));
        orderEntity.setPayType(submitVo.getPayType());
        orderEntity.setSourceType(1);
        orderEntity.setStatus(0);
        orderEntity.setDeliveryCompany(submitVo.getDeliveryCompany());
        orderEntity.setAutoConfirmDay(15);
        // TODO 清算sms中每个商品赠送积分进行汇总
//        orderEntity.setIntegration(0);
//        orderEntity.setGrowth(0);

        UserAddressEntity address = submitVo.getAddress();
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverRegion(address.getRegion());
        orderEntity.setReceiverAddress(address.getAddress());
        orderEntity.setReceiverName(address.getName());

        orderEntity.setDeleteStatus(0);
        orderEntity.setUseIntegration(submitVo.getBounds());

        this.save(orderEntity);
        System.out.println("orderEntity = " + orderEntity);

        // 2.新增订单详情表  (此处应该判断非空 , 没写须知道)
        List<OrderItemEntity> orderItemEntities = items.stream().map(item -> {
            OrderItemEntity orderItemEntity = new OrderItemEntity();
            orderItemEntity.setOrderId(orderEntity.getId());
            orderItemEntity.setOrderSn(submitVo.getOrderToken());

            // 先根据skuid 获取sku
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();

            orderItemEntity.setSkuId(item.getSkuId());
            orderItemEntity.setCategoryId(skuEntity.getCatagoryId());
            orderItemEntity.setSkuName(skuEntity.getName());
            orderItemEntity.setSkuPic(skuEntity.getDefaultImage());
            orderItemEntity.setSkuPrice(skuEntity.getPrice());
            orderItemEntity.setSkuQuantity(item.getCount().intValue());
            // 查询销售属性
            ResponseVo<List<SkuAttrValueEntity>> listResponseVo = this.pmsClient.querySaleAttrValueBySkuId(item.getSkuId());
            List<SkuAttrValueEntity> attrValueEntityList = listResponseVo.getData();
            orderItemEntity.setSkuAttrsVals(JSON.toJSONString(attrValueEntityList));

            // 根据sku里的spuId查询spu
            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();

            orderItemEntity.setSpuId(spuEntity.getId());
            orderItemEntity.setSpuName(spuEntity.getName());
            // 根据品牌id查询品牌
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(spuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            orderItemEntity.setSpuBrand(brandEntity.getName());
            // 根据spu查询描述信息
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
            orderItemEntity.setSpuPic(spuDescEntity.getDecript());

            // 商品促销分解金额,优惠券优惠分解金额,积分优惠分解金额,该商品经过优惠后的分解金额 不做了

//            ResponseVo<SkuBoundsEntity> skuBoundsEntityResponseVo = this.smsClient.queryboundsBySkuId(item.getSkuId());
//            SkuBoundsEntity skuBoundsEntity = skuBoundsEntityResponseVo.getData();
//            orderItemEntity.setGiftIntegration(skuBoundsEntity.getBuyBounds().intValue());
//            orderItemEntity.setGiftGrowth(skuBoundsEntity.getGrowBounds().intValue());

            return orderItemEntity;
        }).collect(Collectors.toList());
        this.orderItemService.saveBatch(orderItemEntities);
        System.out.println("orderItemEntities = " + orderItemEntities);
        // 多设置一个订单详情数据 , 数据更全面
        orderEntity.setItems(orderItemEntities);

        //订单创建好 就开始定时关单
        //oms创建订单 =发送消息=> 延时队列 =ttl=> 死信交换机 => 死信队列 => oms监听死信队列，获取到死信消息后，执行关单操作 => 关闭成功,发送消息给wms解锁库存
        // 和订单创建使用一个本地事务，要么都成功要么都失败。
        this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE", "order.ttl", submitVo.getOrderToken());

        //int i = 1 / 0;

        return orderEntity;
    }

}