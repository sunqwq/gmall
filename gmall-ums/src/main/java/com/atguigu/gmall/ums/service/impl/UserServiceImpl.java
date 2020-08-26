package com.atguigu.gmall.ums.service.impl;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.omg.CORBA.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;


@Slf4j
@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {
    @Autowired
    private RedisTemplate<String,String> redisTemplate;


    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    /**
     * 校验数据是否可用  (手机号 用户名 邮箱的唯一性)
     * data: 要校验的数据
     * type: 1.用户名 2.手机号 3.邮箱
     */
    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        switch (type){
            case 1:wrapper.eq("username", data);break;
            case 2:wrapper.eq("phone", data);break;
            case 3:wrapper.eq("email", data);break;
            default:return null;
        }
        //如果为0  则可以用
        return this.count(wrapper) == 0;
    }

    /**
     * 注册
     */
    @Override
    public void register(UserEntity userEntity, String code) {
        //1. 校验短信验证码
//        String cacheCode = redisTemplate.opsForValue().get(userEntity.getPhone());
//        if (!StringUtils.equals(code, cacheCode)) {
//            return false;
//        }
        // 2.生成盐
        String salt = UUID.randomUUID().toString().substring(0, 6);
        userEntity.setSalt(salt);
        // 3.对密码加密
        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword() + salt));
        // 4.设置创建时间等
        userEntity.setLevelId(1l);
        userEntity.setCreateTime(new Date());
        userEntity.setSourceType(1);
        userEntity.setIntegration(1000);
        userEntity.setGrowth(1000);
        userEntity.setStatus(1);
        // 5.添加到数据库
        this.save(userEntity);


        //注册成功，删除redis中的记录
//        if (save) {
//            this.redisTemplate.delete(userEntity.getPhone());
//        }

    }

    /**
     * 查询用户
     * username/phone/email    password
     */
    @Override
    public UserEntity queryUser(String loginName, String password) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<UserEntity>().eq("username", loginName).or()
                .eq("phone", loginName).or()
                .eq("email", loginName);
        // 1.根据登录名查询用户信息（拿到盐）
        UserEntity userEntity = this.getOne(wrapper);
        // 2.判断用户是否为空
        if (userEntity == null) {
            log.error("账户输入不合法!");
            return userEntity;
        }
        // 3.对密码加盐加密，并和数据库中的密码进行比较
        password = DigestUtils.md5Hex(password + userEntity.getSalt());
        if (!StringUtils.equals(password, userEntity.getPassword())) {
            log.error("密码输入错误！");
            return null;
        }

        return userEntity;
    }

}