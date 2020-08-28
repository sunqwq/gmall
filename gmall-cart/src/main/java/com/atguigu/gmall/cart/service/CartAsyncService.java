package com.atguigu.gmall.cart.service;


import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 提取出 需要异步执行的方法
 */
@Service
public class CartAsyncService {
    @Autowired
    private CartMapper cartMapper;

    //异步更新mysql数据库
    @Async
    public void updateCartByUserIdAndSkuId(String userId , Cart cart) {
        this.cartMapper.update(cart,new UpdateWrapper<Cart>().eq("user_id", userId).eq("sku_id", cart.getSkuId()));
    }

    //异步添加到mysql数据库
    @Async
    public void addCart(Cart cart) {
        this.cartMapper.insert(cart);
    }

    // 5.删除未登录的购物车
    @Async
    public void deleteCartsByUserId(String userKey) {
        this.cartMapper.delete(new QueryWrapper<Cart>().eq("user_id", userKey));

    }

    // 删除购物车的某个商品
    @Async
    public void deleteCartsByUserIdAndSkuId(String userId, Long skuId) {
        this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userId).eq("sku_id", skuId));
    }
}
