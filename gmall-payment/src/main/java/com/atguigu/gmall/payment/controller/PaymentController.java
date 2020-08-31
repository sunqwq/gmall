package com.atguigu.gmall.payment.controller;


import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.config.AlipayTemplate;
import com.atguigu.gmall.payment.entity.PaymentInfoEntity;
import com.atguigu.gmall.payment.service.PaymentService;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import com.atguigu.gmall.payment.vo.PayVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;

@Controller
public class PaymentController {
    @Autowired
    private PaymentService paymentService;
    //引入 阿里支付工具类
    @Autowired
    private AlipayTemplate alipayTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 1. 点击结算跳转到支付选择页面 pay.html
     */
    @GetMapping("pay.html")
    public String topay(@RequestParam("orderToken") String orderToken, Model model) {

        OrderEntity orderEntity = this.paymentService.queryOrderByOrderToken(orderToken);
        if (orderEntity == null) {
            throw new OrderException("该用户对应的订单不存在");
        }
        model.addAttribute("orderEntity",orderEntity);
        return "pay";
    }

    /**
     * 2. 点击立即支付，跳转到对应的支付页面    (支付宝有自己的支付页面alipay.html)
     *
     * 注意:此处返回的String 是from表单字符串,里面有html标签 ,
     * String默认是视图名称,但此处是html片段 , 如何让浏览器作为内容来渲染?
     * 加注解@ResponseBody ,来告诉浏览器,这是一个json字符串,其实不是json,是一个html片段的字符串
     * 加注解 即可作为脚本来渲染 , json也是一种脚本,html也是脚本
     * 最后展示的就是一个html页面
     *
     * ResponseBody 本质不是用来响应Json字符串,只是习惯用于响应,
     * 加上该注解,表示该方法将以其他视图形式来返回,就不是String视图名称来返回
     * json html xml 都是视图形式
     * 主要引入xml相关依赖,ResponseBody还可以用来响应xml
     * 在SpringMvc  xml优先级比json高
     */
    @GetMapping("alipay.html")
    @ResponseBody
    public String alipay(@RequestParam("orderToken") String orderToken) {
        // 1.校验订单的真实性 (查询)
        OrderEntity orderEntity = this.paymentService.queryOrderByOrderToken(orderToken);
        if (orderEntity == null && orderEntity.getStatus() != 0) {
            throw new OrderException("该用户对应的订单不存在！");
        }

        try {
            // 2.整合所需参数
            PayVo payVo = new PayVo();
            payVo.setOut_trade_no(orderToken);  // 订单号
            // StringUtils.lastIndexOf =>查找.在金额中最后一次出现的索引 +3 ==> 保留小数点后3位 ==>最后再从头截取到小数点后三位
            String price = StringUtils.substring(orderEntity.getPayAmount().toString(), 0, StringUtils.lastIndexOf(orderEntity.getPayAmount().toString(), ".") + 3);
            payVo.setTotal_amount(price);
            //payVo.setTotal_amount("0.01");   // 付款金额 , 填一个模拟数据,防止真的支付了
            payVo.setSubject("xxxxxxx");  // 订单名称

            // 3. 生成对账信息 , 记录对账表 payment_info
            Long paymentId = this.paymentService.savepayment(orderEntity,price);
            payVo.setPassback_params(paymentId.toString());   //回调参数 id ,后续可以通过id来查记录表比较数据

            // 3.调用阿里接口 获取from支付表单
            return this.alipayTemplate.pay(payVo);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        throw new OrderException("支付接口调用异常, 请稍后重试!!!");

    }


    /**
     * 3. 用户支付成功之后，跳转到成功页面（同步）并且修改订单的状态 减库存（异步）   paysuccess.html
     */
    @PostMapping("pay/success")
    public String paySuccess(PayAsyncVo payAsyncVo){
        // 1.验签
        Boolean flag = this.alipayTemplate.checkSignature(payAsyncVo);
        if (!flag){
            return "failure";
        }
        // 2.校验业务参数：app_id、out_trade_no、total_amount
        String app_id = payAsyncVo.getApp_id();
        String out_trade_no = payAsyncVo.getOut_trade_no();
        String total_amount = payAsyncVo.getTotal_amount();
        String paymentId = payAsyncVo.getPassback_params();
        if (StringUtils.isBlank(paymentId)) {
            return "failure";
        }
        // 根据id查记录表
        PaymentInfoEntity paymentInfoEntity = this.paymentService.queryPaymentById(paymentId);
        if (!StringUtils.equals(app_id, this.alipayTemplate.getApp_id())
                || !StringUtils.equals(out_trade_no, paymentInfoEntity.getOutTradeNo())
                || paymentInfoEntity.getTotalAmount().compareTo(new BigDecimal(total_amount)) != 0) {
            return "failure";
        }
        // 3.判断交易状态：TRADE_SUCCESS
        if (!StringUtils.equals("TRADE_SUCCESS", payAsyncVo.getTrade_status())) {
            return "failure";
        }
        // 4.完成业务处理
        // 4.1. 更新对账记录，已付款  支付状态，默认值0-未支付，1-已支付。
        this.paymentService.updatePayment(payAsyncVo);

        // 4.2. 更新订单状态，待发货（==>之后再减库存）【0->待付款；1->待发货；2->已发货；3->已完成；4->已关闭；5->无效订单】
        this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE", "order.success", payAsyncVo.getOut_trade_no());

        // 5.返回success
        return "success";
    }

    @GetMapping("pay/ok")
    public String payOk(){

        return "paysuccess";
    }



}
