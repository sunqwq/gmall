package com.atguigu.gmall.ums.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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

     */






}
