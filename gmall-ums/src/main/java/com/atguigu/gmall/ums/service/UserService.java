package com.atguigu.gmall.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.ums.entity.UserEntity;

import java.util.Map;

/**
 * 用户表
 *
 * @author fjy
 * @email 1159213392@qq.com
 * @date 2020-08-21 12:51:49
 */
public interface UserService extends IService<UserEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    /**
     * 校验数据是否可用  (手机号 用户名 邮箱的唯一性)
     * data: 要校验的数据
     * type: 1.用户名 2.手机号 3.邮箱
     */
    Boolean checkData(String data, Integer type);

    /**
     * 注册
     */
    void register(UserEntity userEntity, String code);

    /**
     * 查询用户
     * username/phone/email    password
     */
    UserEntity queryUser(String loginName, String password);
}

