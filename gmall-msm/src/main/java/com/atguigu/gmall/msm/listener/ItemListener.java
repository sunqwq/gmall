package com.atguigu.gmall.msm.listener;



import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


/**
 * 消息队列  消费者
 * 消费者确认机制
 */
@Component
public class ItemListener {

//
//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(value = "MSM-SAVE-QUEUE",durable = "true"),
//            exchange = @Exchange(value = "MSM-ITEM-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
//            key = {"code"}
//    ))
//    public void listener1(String code) {
//
//
//    }

}
