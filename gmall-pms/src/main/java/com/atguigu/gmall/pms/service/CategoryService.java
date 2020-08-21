package com.atguigu.gmall.pms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author fjy
 * @email 1159213392@qq.com
 * @date 2020-08-02 18:33:25
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    /**
     * 根据一级分类id查询二级分类 三级分类
     */
    List<CategoryEntity> queryCategoriesWithSubByPid(Long pid);

    /**
     * 根据三级分类的id查询一二三级分类的集合
     */
    List<CategoryEntity> query123categoriesByCid(Long cid);
}

