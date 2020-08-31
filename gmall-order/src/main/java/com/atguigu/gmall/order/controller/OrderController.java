package com.atguigu.gmall.order.controller;


import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.xml.ws.Response;

@Controller
public class OrderController {
    @Autowired
    private OrderService orderService;


    /**
     * 提交订单
     */
    @PostMapping("submit")
    @ResponseBody
    public ResponseVo<Object> submit(@RequestBody OrderSubmitVo submitVo) {
        OrderEntity orderEntity = this.orderService.submit(submitVo);
        // 把订单号(防重) 传递给支付页面
        return ResponseVo.ok(orderEntity.getOrderSn());

    }

    /**
     *  确认 订单
     */
    @GetMapping("confirm")
    public String confirm(Model model) {
        OrderConfirmVo confirmVo = this.orderService.confirm();
        model.addAttribute("confirmVo", confirmVo);
        return "trade";
    }


    /**
     * 用于 postman 测试
     */
    @GetMapping("confirm2")
    @ResponseBody
    public ResponseVo<OrderConfirmVo> confirm2() {
        OrderConfirmVo confirmVo = this.orderService.confirm();
        return ResponseVo.ok(confirmVo);
    }

}
