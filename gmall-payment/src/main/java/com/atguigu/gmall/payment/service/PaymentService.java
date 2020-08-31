package com.atguigu.gmall.payment.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.Interceptor.LoginInterceptor;
import com.atguigu.gmall.payment.entity.PaymentInfoEntity;
import com.atguigu.gmall.payment.feign.GmallOmsClient;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class PaymentService {
    @Autowired
    private GmallOmsClient omsClient;
    @Autowired
    private PaymentInfoMapper paymentInfoMapper;


    /**
     * 跳转到支付选择页面
     */
    public OrderEntity queryOrderByOrderToken(String orderToken) {
        // 从拦截器中获取userId
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        ResponseVo<OrderEntity> orderEntityResponseVo = this.omsClient.queryOrderByToken(orderToken, userInfo.getUserId());
        return orderEntityResponseVo.getData();
    }

    /**
     * 3. 生成对账信息 , 记录对账表 payment_info ,返回记录表id
     */
    public Long savepayment(OrderEntity orderEntity,String price) {
        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();
        paymentInfoEntity.setPaymentStatus(0);   //支付状态，默认值0-未支付，1-已支付。
        paymentInfoEntity.setCreateTime(new Date());   // 创建时间
        paymentInfoEntity.setTotalAmount(new BigDecimal(price)); // 订单金额
        paymentInfoEntity.setSubject("谷粒商城支付平台");
        paymentInfoEntity.setPaymentType(1);   // 支付类型（微信与支付宝）
        paymentInfoEntity.setOutTradeNo(orderEntity.getOrderSn());

        this.paymentInfoMapper.insert(paymentInfoEntity);
        return paymentInfoEntity.getId();
    }

    // 根据id查记录表
    public PaymentInfoEntity queryPaymentById(String paymentId) {
        //return this.paymentInfoMapper.selectOne(new QueryWrapper<PaymentInfoEntity>().eq("id", paymentId));
        return this.paymentInfoMapper.selectById(paymentId);
    }


    // 4.1. 更新对账记录，1-已支付。
    public void updatePayment(PayAsyncVo payAsyncVo) {
        PaymentInfoEntity paymentInfoEntity = this.paymentInfoMapper.selectById(payAsyncVo.getPassback_params());
        paymentInfoEntity.setPaymentStatus(1);    // 状态 待发货
        paymentInfoEntity.setCallbackTime(new Date()); //回调时间
        paymentInfoEntity.setCallbackContent(JSON.toJSONString(payAsyncVo));
        paymentInfoEntity.setTradeNo(payAsyncVo.getTrade_no());
        this.paymentInfoMapper.updateById(paymentInfoEntity);
    }
}
