package com.atguigu.gmall.oms.listener;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


//消息队列  消费者

/**
 * 消费者确认机制
 */
@Slf4j
@Component
public class OrderListener {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 下单失败后 修改订单状态
     * 订单状态【0->待付款；1->待发货；2->已发货；3->已完成；4->已关闭；5->无效订单】
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "ORDER_FAIL_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.fail"}
    ))
    private void failOrder(String orderToken, Channel channel, Message message) throws IOException {

        try {
            OrderEntity orderEntity = this.orderMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderToken));
            orderEntity.setStatus(5);
            this.orderMapper.update(orderEntity,new UpdateWrapper<OrderEntity>().eq("id", orderEntity.getId()).eq("status", 0));
            // 手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            e.printStackTrace();
            // 判断有没有重试过
            if (message.getMessageProperties().getRedelivered()) {
                // 重试过了就不再重试
                log.error("标记为无效订单失败! 订单编号: ", orderToken);
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                // 没有重试过就重试 (多条false,重试true)
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }

    /* 监听死信队列 , 关闭成功,发送消息给wms解锁库存
         订单状态【0->待付款；1->待发货；2->已发货；3->已完成；4->已关闭；5->无效订单】
     */
    @RabbitListener(queues = "ORDER-DEAD-QUEUE")
    public void closeOrder(String orderToken, Channel channel, Message message) throws IOException {

        try {
            OrderEntity orderEntity = this.orderMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderToken));
            orderEntity.setStatus(4);
            // 如果关闭成功,发送消息给wms解锁库存
            if (this.orderMapper.update(orderEntity,new UpdateWrapper<OrderEntity>().eq("id", orderEntity.getId()).eq("status", 0)) == 1) {
                // 发送消息
                this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE", "stock.unlock", orderToken);
            }
            // 手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            e.printStackTrace();
            // 判断有没有重试过
            if (message.getMessageProperties().getRedelivered()) {
                // 重试过了就不再重试
                log.error("关闭订单失败! 订单编号: ", orderToken);
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                // 没有重试过就重试 (多条false,重试true)
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }

    /**
     * 下单成功后 修改订单状态
     * 订单状态1 【0->待付款；1->待发货；2->已发货；3->已完成；4->已关闭；5->无效订单】
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "ORDER-SUCCESS-QUEUE",durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"order.success"}
    ))
    public void successOrder(String orderToken, Channel channel,Message message) throws IOException {
        try {
            // 更新订单状态
            if (this.orderMapper.success(orderToken) == 1) {
                // 发送消息给wms减库存
                this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE","stock.minus",orderToken);
                // 发送消息给ums加积分 TODO   此处没有继续往下,因为没填积分,成长值
                OrderEntity orderEntity = this.orderMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderToken));
                Map<String, Object> map = new HashMap<>();
                map.put("userId", orderEntity.getUserId());
                map.put("integration", orderEntity.getIntegration());
                map.put("growth", orderEntity.getGrowth());

                //this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE","user.bounds",map);
            }
            // 手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
            // 判断有没有重试过
            if (message.getMessageProperties().getRedelivered()) {
                // 重试过了就不再重试
                log.error("关闭订单失败! 订单编号: ", orderToken);
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                // 没有重试过就重试 (多条false,重试true)
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }

    }

}
