package com.atguigu.gmall.search.feign;

import com.atguigu.gmall.wms.api.GmallWmsApi;
import org.springframework.cloud.openfeign.FeignClient;

//继承接口即可

@FeignClient("wms-service")
public interface GmallWmsClient extends GmallWmsApi {
}
