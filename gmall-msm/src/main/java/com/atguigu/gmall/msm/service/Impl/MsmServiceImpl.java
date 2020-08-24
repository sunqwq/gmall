package com.atguigu.gmall.msm.service.Impl;

import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.atguigu.gmall.msm.service.MsmService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
public class MsmServiceImpl implements MsmService {
    //发送短信的方法
    @Override
    public Boolean send(Map<String, Object> map, String phone) {

        if(StringUtils.isEmpty(phone)) return false;

        //设置为自己的(子账号)
        DefaultProfile profile =
                DefaultProfile.getProfile("default", "LTAI4GBedUGfPjC5EN2SC1dr", "lqIrxTpHiYYiiYZpml9omPfliihrss");
        IAcsClient client = new DefaultAcsClient(profile);

        //设置相关固定的参数(不改)
        CommonRequest request = new CommonRequest();
        //request.setProtocol(ProtocolType.HTTPS);
        request.setMethod(MethodType.POST);
        request.setDomain("dysmsapi.aliyuncs.com");
        request.setVersion("2017-05-25");
        request.setAction("SendSms");

        //设置发送相关的参数
        request.putQueryParameter("PhoneNumbers", phone); //手机号
        request.putQueryParameter("SignName", "谷粒学院在线教育网站");  //申请阿里云 签名名称
        request.putQueryParameter("TemplateCode", "SMS_197611188");   //申请阿里云 模版CODE
        //fastjson 转换工具
        request.putQueryParameter("TemplateParam", JSONObject.toJSONString(map)); //验证码数据  需要json数据   map转换为json数据

        //最终发送
        try {
            CommonResponse response = client.getCommonResponse(request);
            boolean success = response.getHttpResponse().isSuccess();
            return success;
        } catch (ClientException e) {
            e.printStackTrace();
            return false;
        }
    }
}
