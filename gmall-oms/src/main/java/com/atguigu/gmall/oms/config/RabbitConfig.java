package com.atguigu.gmall.oms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;


/**
 * 生产者确认机制
 */
@Configuration
@Slf4j
public class RabbitConfig {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 构造后执行方法
    @PostConstruct
    public void init() {
        // 确认消息是否到达交换机
        this.rabbitTemplate.setConfirmCallback((correlationData,ack, cause) -> {
            if (ack) {
                System.out.println(" 消息到达交换机 ");
            } else {
                log.error(" 消息没有到达交换机 ");
            }
        });
        // 确认是否到达队列，(只有消息未到达队列才会执行)
        this.rabbitTemplate.setReturnCallback((Message message, int replyCode, String replyText, String exchange, String routingKey) -> {
            log.error("消息未到达队列,交换机是:{},路由键:{},消息内容:{}",exchange,routingKey,new String(message.getBody()));
        });

    }

    /**死信队列
     * 延时队列  =>和普通的队列有何区别：
     * 				1.队列会指定消息的生存时间：x-message-ttl:毫秒值的生存时间
     * 				2.队列没有消费者
     * x-message-ttl：指定TTL时间
     * x-dead-letter-exchange：死信转发所需的死信交换机（DLX）
     * x-dead-letter-routing-key：转发死信时的routingKey（DLK）

     死信消息产生的3个场景：
     1.basicNack或者basicReject拒绝了消息又不重新入队
     2.队列中的消息达到TTL
     3.队列已满依然入队

     rabbitmq的延时队列和死信队列结合（推荐）  ==>解决定时关单

     oms创建订单 =发送消息=> 延时队列 =ttl=> 死信交换机 => 死信队列
     => oms监听死信队列，获取到死信消息后，执行关单操作 => 关闭成功,发送消息给wms解锁库存
     */
    // 1.1配置延时队列
    @Bean
    public Queue ttlQueue() {
        return QueueBuilder.durable("ORDER-CLOSE-QUEUE")
                .withArgument("x-message-ttl",90000)
                .withArgument("x-dead-letter-exchange","ORDER-EXCHANGE")
                .withArgument("x-dead-letter-routing-key","order.dead").build();
    }

    // 1.2将延时队列绑定到死信交换机 DLX
    @Bean
    public Binding ttlBinding(Queue ttlQueue) {
        return new Binding("ORDER-CLOSE-QUEUE", Binding.DestinationType.QUEUE,
                "ORDER-EXCHANGE","order.ttl",null);
    }


    // 2.1配置死信队列 DLQ
    @Bean
    public Queue deadQueue() {
        return QueueBuilder.durable("ORDER-DEAD-QUEUE").build();
    }

    // 2.2将死信队列绑定到交换机 DLX
    @Bean
    public Binding deadBinding(Queue deadQueue) {
        return new Binding("ORDER-DEAD-QUEUE",Binding.DestinationType.QUEUE,
                "ORDER-EXCHANGE","order.dead",null);
    }

}
