package com.atguigu.gmall.sms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 属于sms
 * 专门提供api接口(feign)
 * 由调用者继承
 */

public interface GmallSmsApi {

    @PostMapping("sms/skubounds/sku/sales")
    public ResponseVo<Object> saveSkuSales(@RequestBody SkuSaleVo skuSaleVo);
}
