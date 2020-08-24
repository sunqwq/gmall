package com.atguigu.gmall.msm.controller;


import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.msm.service.MsmService;
import com.atguigu.gmall.msm.utils.RandomUtil;
import com.baomidou.mybatisplus.extension.api.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Api(tags = "阿里云短信发送管理")
@RestController
@RequestMapping("msm")
public class MsmController {

    @Autowired
    private MsmService msmService;
    //springboot整合redis后 封装的对象 redisTemplate
    //此处需要注意:Redistemplate 存的时候和取的时候 泛型必须一致
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;


    //发送短信的方法
    //    @GetMapping("send/{phone}")
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "MSM-SAVE-QUEUE",durable = "true"),
            exchange = @Exchange(value = "MSM-ITEM-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"code"}
    ))
    public ResponseVo sendMsm(String phone) {
        //1.先从redis获取验证码,如果获取到直接返回
        String code = redisTemplate.opsForValue().get(phone);
        if (!StringUtils.isEmpty(code)) {
            return ResponseVo.ok();
        }

        //2.如果获取不到,  进行阿里云发送
        //通过工具类  生成四位随机值,传递阿里云进行发送
        code = RandomUtil.getFourBitRandom();

        Map<String,Object> map = new HashMap<>();
        map.put("code",code);
        //调用service发送短信的方法
        Boolean idSend = msmService.send(map,phone);
        if (idSend) {
            //发送成功 把发送成功验证码放到redis中 并设置有效时间5分钟
            redisTemplate.opsForValue().set(phone,code, 5, TimeUnit.MINUTES);


            return ResponseVo.ok();
        } else {
            return ResponseVo.fail("短信发送失败");
        }

    }


}
