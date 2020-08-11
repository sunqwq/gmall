package com.atguigu.gmall.pms.feign;

import com.atguigu.gmall.sms.api.GmallSmsApi;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * feign 的最佳实践
 *   由服务提供商提供api接口
 *   调用方直接继承即可
 */

@Component
@FeignClient("sms-service")
public interface GmallSmsClient extends GmallSmsApi {

}
