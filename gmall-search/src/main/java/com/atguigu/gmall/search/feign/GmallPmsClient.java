package com.atguigu.gmall.search.feign;

import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

//继承接口即可

@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {
}
