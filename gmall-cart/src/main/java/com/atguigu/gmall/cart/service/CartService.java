package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.Interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.UserException;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jdk.nashorn.internal.scripts.JD;
import org.hibernate.validator.internal.IgnoreForbiddenApisErrors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartService {
    @Autowired
    private CartAsyncService cartAsyncService;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "cart:info:";

    private static final String PRICE_PREFIX = "cart:price:";

    /*加入购物车  map(key,map(skuId,value))   里面的值都是string类型
        已登录,userId做key;
        未登录,userKey做key
        内层key都是skuId ,value是购物车对象Cart
     */
    public void saveCart(Cart cart) {
        // 1.获取用户的登录信息
        String userId = null;
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if (userInfo.getUserId() != null) {
            // 已登录,userId就是userId ,   userId做key;
            userId = userInfo.getUserId().toString();
        } else {
            // 未登录,userId指的就是userKey   , userKey做key
            userId = userInfo.getUserKey();
        }
        String key = KEY_PREFIX + userId;
        // 2.通过外层的key ,获取内层map的操作对象(该用户所有购物车的集合8:cart,9:cart) map(key,hashOps(skuId,value))
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        // 判断该用户购物车中 是否已有该商品
        String skuId = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();
        if (hashOps.hasKey(skuId)) {
            // 有则更新数量
            // 获取value (cart对象 json类型)
            String cartJson = hashOps.get(skuId).toString();
            //反序列化
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(cart.getCount().add(count));

            //重新放入redis和mysql
            this.cartAsyncService.updateCartByUserIdAndSkuId(userId,cart);
        } else {
            // 无则新增一条记录 (skuId,count在路径中携带 已有)
            cart.setUserId(userId);
            cart.setCheck(true);

            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                cart.setTitle(skuEntity.getTitle());
                cart.setDefaultImage(skuEntity.getDefaultImage());
                cart.setPrice(skuEntity.getPrice());
            }

            ResponseVo<List<WareSkuEntity>> wareSkuBySkuId = this.wmsClient.queryWareSkuBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareSkuBySkuId.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
            // 销售属性：List<SkuAttrValueEntity>的json格式
            ResponseVo<List<SkuAttrValueEntity>> SkuAttrValueEntities = this.pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntityList = SkuAttrValueEntities.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntityList)) {
                cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntityList));
            }
            // 营销信息: List<ItemSaleVo>的json格式
            ResponseVo<List<ItemSaleVo>> listResponseVo = this.smsClient.querysalesByskuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = listResponseVo.getData();
            if (!CollectionUtils.isEmpty(itemSaleVos)) {
                cart.setSales(JSON.toJSONString(itemSaleVos));
            }

            //mysql新增cart
            this.cartAsyncService.addCart(cart);

            // 并且新增价格的缓存，如果已经有人把该商品加入了购物车，该商品的价格缓存已存在。这时依然进行加缓存，相当于做了价格同步
            this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuId, skuEntity.getPrice().toString());
        }
        // redis中 ,新增和更新都是put 方法
        hashOps.put(skuId,JSON.toJSONString(cart));
    }

    /*
    根据skuId查询购物车  map(key,map(skuId,value))
     */
    public Cart queryCartBySkuId(Long skuId) {
        String key = KEY_PREFIX;
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if (userInfo.getUserId() != null) {
            key += userInfo.getUserId();
        } else {
            key += userInfo.getUserKey();
        }
        //通过外层key 获取内层map
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        //如果存在
        if (hashOps.hasKey(skuId.toString())) {
            //根据内层skuId查询购物车
            String cartJson = hashOps.get(skuId.toString()).toString();
            return JSON.parseObject(cartJson, Cart.class);
        } else {
            throw new UserException("您的购物车中没有该商品记录！");
        }
    }


    /**
     * 购物车结算时 列出 购物车数据
     */
    public List<Cart> queryCarts() {
        // 1.查询未登录的购物车
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        String unLoginkey = KEY_PREFIX + userKey;
        BoundHashOperations<String, Object, Object> unLoginHashOps = this.redisTemplate.boundHashOps(unLoginkey);
        // 获取未登录的购物车集合
        List<Object> unLoginCartJsons  = unLoginHashOps.values();
        // 反序列化
        List<Cart> unLoginCarts  = null;
        if (!CollectionUtils.isEmpty(unLoginCartJsons)) {
            unLoginCarts  = unLoginCartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                // 查询redis中的实时价格缓存设置给查询结果集
                cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }

        // 2.判断登录状态
        Long userId = userInfo.getUserId();
        if (userId == null) {
            // 3.未登录，返回未登录的购物车
            return unLoginCarts;
        }

        // 4.登录，合并未登录的购物车到登录状态的购物车
        String loginKey = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);
        if (!CollectionUtils.isEmpty(unLoginCarts)) {
            unLoginCarts.forEach(cart -> {
                //未登录的购物车的商品数量
                BigDecimal count = cart.getCount();
                //有相同的商品就合并数量
                if (loginHashOps.hasKey(cart.getSkuId().toString())) {
                    String cartJson = loginHashOps.get(cart.getSkuId().toString()).toString();
                    cart = JSON.parseObject(cartJson, Cart.class); //登录的购物车的商品数量
                    cart.setCount(cart.getCount().add(count));
                    //更新mysql的数量
                    this.cartAsyncService.updateCartByUserIdAndSkuId(userId.toString(),cart);
                } else {
                    //没有相同的商品就新增
                    cart.setUserId(userId.toString());
                    //新增mysql的数量
                    this.cartAsyncService.addCart(cart);
                }
                //更新redis的数量
                loginHashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
            });
        }

        // 5.删除未登录的购物车
        this.cartAsyncService.deleteCartsByUserId(userKey);
        this.redisTemplate.delete(unLoginkey);
        // 6.查询登录状态的购物车
        List<Object> loginCartJsons = loginHashOps.values();
        if (!CollectionUtils.isEmpty(loginCartJsons)) {
            //直接反序列化成新的集合后返回
            return loginCartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                // 查询redis中的实时价格缓存设置给查询结果集
                cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId())));

                return cart;
            }).collect(Collectors.toList());
        }
        return null;
    }

    /*
     * 更新购物车的数量
     */
    public void updateNum(Cart cart) {
        // 获取登录状态
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = null;
        if (userInfo.getUserId() != null) {
            userId = userInfo.getUserId().toString();
        } else {
            userId = userInfo.getUserKey();
        }
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        BigDecimal count = cart.getCount();
        if (hashOps.hasKey(cart.getSkuId().toString())) {
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(count);
            //更新mysql和redis

            this.cartAsyncService.updateCartByUserIdAndSkuId(userId,cart);
            hashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));

        }
    }

    /**
     * 更新购物车的状态
     *
     */
    public void updateStatus(Cart cart) {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = null;
        if (userInfo.getUserId() != null) {
            userId = userInfo.getUserId().toString();
        } else {
            userId = userInfo.getUserKey();
        }
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        Boolean check = cart.getCheck();
        if (hashOps.hasKey(cart.getSkuId().toString())) {
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCheck(check);
            //更新mysql和redis
            this.cartAsyncService.updateCartByUserIdAndSkuId(userId,cart);
            hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));

        }

    }

    /**
     * 删除购物车的某个商品
     */
    public void deleteCart(Long skuId) {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = null;
        if (userInfo.getUserId() != null) {
            userId = userInfo.getUserId().toString();
        } else {
            userId = userInfo.getUserKey();
        }
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        if (hashOps.hasKey(skuId.toString())) {
            //删除mysql和redis中对应的商品
            this.cartAsyncService.deleteCartsByUserIdAndSkuId(userId,skuId);
            hashOps.delete(skuId.toString());
        }
    }


//===================================
    /**
     * 测试Spring Task
     * 优雅的编写多线程程序  (@EnableAsync开启异步功能 , @Async标记异步调用方法)
     */
    @Async  //标记异步调用方法
    public String executor1(){
        try {
            System.out.println("这是service中executor1方法开始执行。。。。");
            TimeUnit.SECONDS.sleep(4);
            System.out.println("这是service中executor1方法执行完成。。。。");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 通过AsyncResult返回方法的返回结果集
        return "hello executor1";
    }

    @Async   //标记异步调用方法
    public String executor2(){
        try {
            System.out.println("这是service中executor2方法开始执行。。。。");
            TimeUnit.SECONDS.sleep(5);
            int i = 1/0;
            System.out.println("这是service中executor2方法执行完成。。。。");
        } catch (InterruptedException e) {
            e.printStackTrace();

        }
        return "hello executor2";
    }

//使用ListenableFuture对象来实现 自定义异步回调 ============================
    @Async
    public ListenableFuture<String> executor3(){
        try {
            System.out.println("这是service中executor1方法开始执行。。。。");
            TimeUnit.SECONDS.sleep(4);
            System.out.println("这是service中executor1方法执行完成。。。。");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 通过AsyncResult返回方法的返回结果集
        return AsyncResult.forValue("hello executor1");
    }

    @Async
    public ListenableFuture<String> executor4(){
        try {
            System.out.println("这是service中executor2方法开始执行。。。。");
            TimeUnit.SECONDS.sleep(5);
            int i = 1/0;
            System.out.println("这是service中executor2方法执行完成。。。。");
        } catch (Exception e) {
            e.printStackTrace();
            //返回异常
            return AsyncResult.forExecutionException(e);
        }
        return AsyncResult.forValue("hello executor2");
    }


}
