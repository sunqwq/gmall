package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;


//消息队列  消费者
/**
 * 消费者确认机制
 */
@Slf4j
@Component
public class StockListener {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WareSkuMapper wareSkuMapper;

    private static final String KEY_PREFIX = "wms:lock:";

    /**
     * 下单失败后 解锁库存  order.fail
     *
     * oms 监听死信队列 , 关闭成功,发送消息给wms解锁库存  stock.unlock
     * wms 监听死信队列 , 下单锁定库存后,发送消息给wms  2分钟后解锁库存  stock.unlock
     */

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "ORDER_STOCK_QUEUE",durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"order.fail","stock.unlock"}
    ))
    public void unlockstock(String orderToken, Channel channel, Message message) throws IOException {
        String skuLockString = this.redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        // 如果没有缓存的库存信息,则直接返回
        if (StringUtils.isBlank(skuLockString)) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }

        try {
            //不为空,则解锁库存
            List<SkuLockVo> skuLockVos = JSON.parseArray(skuLockString, SkuLockVo.class);
            skuLockVos.forEach(skuLockVo -> {
                this.wareSkuMapper.unlock(skuLockVo.getWareSkuId(),skuLockVo.getCount());
            });

            //解锁库存后 ,删除redis中锁定库存的缓存 (防止重复解锁库存)
            this.redisTemplate.delete(KEY_PREFIX + orderToken);

            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            e.printStackTrace();
            // 判断有没有重试过
            if (message.getMessageProperties().getRedelivered()) {
                // 重试过了就不再重试
                log.error("解锁库存的消息消费失败! 订单编号: ",orderToken);
                channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
            } else {
                // 没有重试过就重试 (多条false,重试true)
                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false, true);
            }
        }
    }


    /**
     * 用户支付成功之后 , 发送消息给wms减库存
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "STOCK-MINUS-QUEUE",durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"stock.minus"}
    ))
    public void minusStock(String orderToken, Channel channel, Message message) throws IOException {
        String skuLockString = this.redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        // 如果没有缓存的库存信息,则直接返回
        if (StringUtils.isBlank(skuLockString)) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }

        try {
            //不为空 ,减库存
            List<SkuLockVo> skuLockVos = JSON.parseArray(skuLockString, SkuLockVo.class);
            skuLockVos.forEach(skuLockVo -> {
                // 遍历 减库存
                this.wareSkuMapper.minusStock(skuLockVo.getWareSkuId(),skuLockVo.getCount());
            });
            //减库存 ,删除redis中锁定库存的缓存 (防止重复减库存)
            this.redisTemplate.delete(KEY_PREFIX + orderToken);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
            // 判断有没有重试过
            if (message.getMessageProperties().getRedelivered()) {
                // 重试过了就不再重试
                log.error("减库存的消息消费失败! 订单编号: ",orderToken);
                channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
            } else {
                // 没有重试过就重试 (多条false,重试true)
                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false, true);
            }
        }

    }


}
