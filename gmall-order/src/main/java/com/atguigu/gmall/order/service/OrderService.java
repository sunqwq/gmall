package com.atguigu.gmall.order.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.common.exception.UserException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.Interceptor.LoginInterceptor;
import com.atguigu.gmall.order.config.OrderCache;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 异步编排CompletableFuture
 *  *  		初始化方法：
 *  * 			supplyAsync：任务有返回结果集
 *  * 				supplyAsync(() -> {
 *  *                                })
 *  * 			runAsync：没有返回结果集
 *  * 				runAsync(() -> {
 *  *                })
 *  *
 *  * 		计算完成方法：
 *  * 			whenComplete()
 *  * 			whenCompleteAsync()
 *  * 				上述方法：上一个任务正常执行或者出现异常时都可以执行该子任务
 *  * 			exceptionnally()
 *  * 				上一个任务出现异常时会执行该子任务
 *  *
 *  * 		串行化方法：
 *  * 			thenApply：可以获取上一个任务的返回结果，并给下一个任务返回自己的结果
 *  * 			thenAccept：可以获取上一个任务的返回结果，但是不会给下一个任务返回自己的结果
 *  * 			thenRun：上一个任务执行完成即执行该任务，既不获取上一个任务的返回结果，也不给下一个任务返回自己的结果
 *  * 			都有异步方法，并且有异步同载方法。
 *  *
 *  * 		组合方法：
 *  * 			allOf：所有任务都完成才执行新任务
 *  * 			anyOf：任何一个任务完成执行新任务
 */
@Service
public class OrderService {
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallCartClient cartClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    private static final String KEY_PREFIX = "order:token:";

    /**
     * 订单确认
     *    (从购物车中只能获取skuId和count,其余字段实时获取,购物车中的字段可能滞后)
     */
    public OrderConfirmVo confirm() {
        OrderConfirmVo confirmVo = new OrderConfirmVo();

        //从拦截器中获取userId
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        if (userId == null) {
            return null;
        }

        CompletableFuture<Void> addressCompletableFuture = CompletableFuture.runAsync(() -> {
            // 1.获取用户的收货地址列表， 根据用户id查询收货地址列表
            ResponseVo<List<UserAddressEntity>> queryAddressByUserId = this.umsClient.queryAddressByUserId(userId);
            List<UserAddressEntity> userAddressEntities = queryAddressByUserId.getData();
            confirmVo.setAddresses(userAddressEntities);
        }, threadPoolExecutor);

        CompletableFuture<List<Cart>> cartCompletableFuture = CompletableFuture.supplyAsync(() -> {
            // 2.从购物车中查询用户选中的购物车记录
            ResponseVo<List<Cart>> cartByUserId = this.cartClient.queryCheckedCartByUserId(userId);
            List<Cart> carts = cartByUserId.getData();
            if (CollectionUtils.isEmpty(carts)) {
                throw new UserException(" 你没有选中的购物车记录，请先要购买的商品！ ");
            }
            return carts;
        }, threadPoolExecutor);

        CompletableFuture<Void> orderCompletableFuture = cartCompletableFuture.thenAcceptAsync(carts -> {
            List<OrderItemVo> orderItemVos = carts.stream().map(cart -> {
                OrderItemVo orderItemVo = new OrderItemVo();

                CompletableFuture<Void> skuCompletableFuture = CompletableFuture.runAsync(() -> {
                    ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
                    SkuEntity skuEntity = skuEntityResponseVo.getData();
                    if (skuEntity != null) {
                        orderItemVo.setSkuId(cart.getSkuId());
                        orderItemVo.setCount(cart.getCount());
                        orderItemVo.setDefaultImage(skuEntity.getDefaultImage());
                        orderItemVo.setTitle(skuEntity.getTitle());
                        orderItemVo.setPrice(skuEntity.getPrice());
                        orderItemVo.setWeight(new BigDecimal(skuEntity.getWeight()));
                    }
                }, threadPoolExecutor);

                CompletableFuture<Void> wareCompletableFuture = CompletableFuture.runAsync(() -> {
                    ResponseVo<List<WareSkuEntity>> wareSkuBySkuId = this.wmsClient.queryWareSkuBySkuId(cart.getSkuId());
                    List<WareSkuEntity> wareSkuEntities = wareSkuBySkuId.getData();
                    if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                        orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                    }
                }, threadPoolExecutor);

                CompletableFuture<Void> itemCompletableFuture = CompletableFuture.runAsync(() -> {
                    ResponseVo<List<ItemSaleVo>> itemSaleVo = this.smsClient.querysalesByskuId(cart.getSkuId());
                    List<ItemSaleVo> itemSaleVoList = itemSaleVo.getData();
                    orderItemVo.setSales(itemSaleVoList);
                }, threadPoolExecutor);

                CompletableFuture<Void> saleAttrCompletableFuture = CompletableFuture.runAsync(() -> {
                    ResponseVo<List<SkuAttrValueEntity>> listResponseVo = this.pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
                    List<SkuAttrValueEntity> skuAttrValueEntities = listResponseVo.getData();
                    orderItemVo.setSaleAttrs(skuAttrValueEntities);
                }, threadPoolExecutor);

                CompletableFuture.allOf(skuCompletableFuture, wareCompletableFuture, itemCompletableFuture, saleAttrCompletableFuture).join();

                return orderItemVo;

            }).collect(Collectors.toList());
            confirmVo.setItems(orderItemVos);

        }, threadPoolExecutor);

        // 3.查询用户信息，并且获取用户中的购物积分
        CompletableFuture<Void> userCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
            UserEntity userEntity = userEntityResponseVo.getData();
            confirmVo.setBounds(userEntity.getIntegration());
        }, threadPoolExecutor);

        /**
         * IdWorker 基于雪花算法 每秒生成26.5W不重复的Id
         */
        // 4.防重，生成唯一标识，响应给页面，保存到redis中一份,防止出现幂等性问题(只能防止,不能解决)
        CompletableFuture<Void> tokenCompletableFuture = CompletableFuture.runAsync(() -> {
            String orderToken = IdWorker.getTimeId();
            confirmVo.setOrderToken(orderToken);

            this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, orderToken,3, TimeUnit.HOURS);

        }, threadPoolExecutor);

        CompletableFuture.allOf(addressCompletableFuture,orderCompletableFuture,userCompletableFuture,tokenCompletableFuture).join();

        return confirmVo;

    }


    /**
     * 提交订单
     */

    public OrderEntity submit(OrderSubmitVo submitVo) {
        // 1.防重
        String orderToken = submitVo.getOrderToken();
        if (StringUtils.isBlank(orderToken)){
            throw new OrderException("^_^");
        }
        // 比较防重标识,相同就删除redis中的记录（lua脚本）
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                "then return redis.call('del', KEYS[1]) " +
                "else return 0 end";
        Boolean flag = redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIX + orderToken), orderToken);
        if (!flag){
            throw new OrderException("请不要重复提交！");
        }

        // 2.验价
        BigDecimal totalPrice = submitVo.getTotalPrice();
        List<OrderItemVo> items = submitVo.getItems();
        if (CollectionUtils.isEmpty(items)) {
            throw new OrderException("请选择要购买的商品！");
        }
        // 获取实时总价格
        BigDecimal currentTotalPrice  = items.stream().map(item -> {
            // 根据skuId查询数据库中的实时单价
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                return new BigDecimal(0);
            }
            return skuEntity.getPrice().multiply(item.getCount());
        }).reduce((a, b) -> a.add(b)).get();
        // totalPrice 和 实时总价格currentTotalPrice 比较 -1小于 0等于 1大于
        if (totalPrice.compareTo(currentTotalPrice) != 0) {
            throw new OrderException("页面已过期，请刷新后再试！");
        }

        // 3.验库存并锁库存  ( 锁定成功 , 锁定成功 集合为null)
        // 事务问题: 发送成功,响应失败
        List<SkuLockVo> lockVos = items.stream().map(item -> {
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(item.getSkuId());
            skuLockVo.setCount(item.getCount().intValue());
            return skuLockVo;
        }).collect(Collectors.toList());
        ResponseVo<List<SkuLockVo>> skuLockResponseVo = this.wmsClient.checkAndLock(lockVos, orderToken);
        List<SkuLockVo> skuLockVoList = skuLockResponseVo.getData();
        if (!CollectionUtils.isEmpty(skuLockVoList)) {
            throw new OrderException(JSON.toJSONString(skuLockVoList));
        }

        /**
         * 如果下单异常,第三步要回滚,分布式事务
         * 1. 2PC  seata  性能低,安全高,强一致性
         * 2. TCC
         * 3. MQ          性能高,安全低
         */
        // 4.下单操作（新增订单表,订单详情表）
        Long userId = null;
        OrderEntity orderEntity = null;
        try {
            UserInfo userInfo = LoginInterceptor.getUserInfo();
            userId = userInfo.getUserId();
            ResponseVo<OrderEntity> orderEntityResponseVo = this.omsClient.saveOrder(submitVo, userId);
            orderEntity = orderEntityResponseVo.getData();
            System.out.println("orderEntity = " + orderEntity);
        } catch (Exception e) {
            e.printStackTrace();
            // 发送消息给库存wms和订单oms，解锁库存并修改订单状态  (回滚)
            this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE", "order.fail", orderToken);
        }

        // 5.发消息给购物车，异步删除对应购物车信息  (MQ)
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", userId);
            List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            map.put("skuIds",JSON.toJSONString(skuIds));
            this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE", "cart.delete", map);
        } catch (AmqpException e) {
            e.printStackTrace();
        }

        return orderEntity;
    }
}
