package com.atguigu.gmall.msm.service;

import java.util.Map;

public interface MsmService {

    //发送短信的方法
    Boolean send(Map<String, Object> map, String phone);


}
