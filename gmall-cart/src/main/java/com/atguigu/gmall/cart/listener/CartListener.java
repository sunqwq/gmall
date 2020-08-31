package com.atguigu.gmall.cart.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import sun.rmi.runtime.Log;

import java.io.IOException;
import java.util.List;
import java.util.Map;


//消息队列  消费者
/**
 * 消费者确认机制
 */
@Component
@Slf4j
public class CartListener {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String PRICE_PREFIX = "cart:price:";

    private static final String KEY_PREFIX = "cart:info:";

    // 收到消息 更新实时价格
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART-ITEM-QUEUE",durable = "true"),
            exchange = @Exchange(value = "PMS-ITEM-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"item.update"}
    ))
    public void listener(Long spuId, Channel channel, Message message) throws IOException {
        ResponseVo<List<SkuEntity>> listResponseVo = this.pmsClient.querySkuBySpuId(spuId);
        List<SkuEntity> skuEntityList = listResponseVo.getData();
        if (!CollectionUtils.isEmpty(skuEntityList)) {
            skuEntityList.forEach(skuEntity -> {
                //setIfPresent 如果有key才设置,没有不设置
                this.redisTemplate.opsForValue().setIfPresent(PRICE_PREFIX + skuEntity.getId(), skuEntity.getPrice().toString());
            });
        }
        //确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }


    // 收到消息后，删除对应购物车信息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "ORDER_CART_QUEUE",durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"cart.delete"}
    ))
    public void deleteCart(Map<String,Object> map,Message message,Channel channel) throws IOException {
        if (CollectionUtils.isEmpty(map)) {
            // 手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }
        try {
            String userId = map.get("userId").toString();
            String skuIdJson = map.get("skuIds").toString();
            List<String> skuIds = JSON.parseArray(skuIdJson, String.class);
            BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
            // 删除对应购物车信息
            hashOps.delete(skuIds.toArray());
            // 手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
             e.printStackTrace();
            // 判断有没有重试过
            if (message.getMessageProperties().getRedelivered()) {
                // 重试过了就不再重试
                log.error("消息消费失败! 下单成功之后删除购物车失败,消息内容{}",map.toString());
                channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
            } else {
                // 没有重试过就重试 (多条false,重试true)
                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false, true);
            }
        }


    }


}
