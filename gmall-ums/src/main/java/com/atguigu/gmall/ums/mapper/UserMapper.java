package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author fjy
 * @email 1159213392@qq.com
 * @date 2020-08-21 12:51:49
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
