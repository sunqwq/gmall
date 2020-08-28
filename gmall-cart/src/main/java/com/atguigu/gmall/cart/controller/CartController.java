package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.Interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.UserException;
import lombok.NonNull;
import org.omg.PortableInterceptor.INACTIVE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.Response;
import java.util.List;

@Controller
public class CartController {
    @Autowired
    private CartService cartService;

    /*
    加入购物车   (skuId,count在路径中携带 已有)
     */
    @GetMapping
    public String saveCart(Cart cart) {
        if (cart == null || cart.getSkuId() == null) {
            throw new UserException("你没有选中任何商品!");
        }
        this.cartService.saveCart(cart);
        return "redirect:http://cart.gmall.com/addCart?skuId=" + cart.getSkuId();
    }

    /*
    根据skuId查询购物车
     */
    @GetMapping("addCart")
    public String queryCartBySkuId(@RequestParam("skuId") Long skuId, Model model) {
        if (skuId == null) {
            throw new UserException("购物车中没有任何商品!");
        }
        Cart cart = this.cartService.queryCartBySkuId(skuId);
        model.addAttribute("cart", cart);
        return "addCart";
    }

    /**
     * 购物车结算时 列出 购物车数据
     */
    @GetMapping("cart.html")
    public String queryCarts(Model model) {
        List<Cart> carts = this.cartService.queryCarts();
        model.addAttribute("carts",carts);
        return "cart";
    }

    /*
     * 更新购物车的数量
     */
    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo<Object> updateNum(@RequestBody Cart cart) {
        this.cartService.updateNum(cart);
        return ResponseVo.ok();
    }


    /**
     * 更新购物车的状态
     *
     */
    @PostMapping("updateStatus")
    @ResponseBody
    public ResponseVo<Object> updateStatus(@RequestBody Cart cart) {
        this.cartService.updateStatus(cart);
        return ResponseVo.ok();
    }

    /**
     * 删除购物车
     */
    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo<Object> deleteCart(@RequestParam("skuId")Long skuId) {
        this.cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }


//测试用====================================
    @GetMapping("test")
    @ResponseBody
    public String test(HttpServletRequest request) {

        long now = System.currentTimeMillis();
        System.out.println(" test方法开始执行.... ");
        this.cartService.executor1();
        this.cartService.executor2();

//        this.cartService.executor3().addCallback(
//                t -> System.out.println("异步成功回调: " + t),
//                ex -> System.out.println(" 异步失败回调: " + ex.getMessage()));
//        this.cartService.executor4().addCallback(
//                t -> System.out.println("异步成功回调: " + t),
//                ex -> System.out.println(" 异步失败回调: " + ex.getMessage()));
        System.out.println(" test方法执行结束..... ,所花时间: " + (System.currentTimeMillis() - now));
        //System.out.println(request.getAttribute("userId"));
        ///System.out.println(request.getAttribute("userKey"));

        //System.out.println(LoginInterceptor.getUserInfo());
        return "test>...";
    }

}
