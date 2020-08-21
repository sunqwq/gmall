package com.atguigu.gmall.pms.service.impl;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    /**
     * 根据一级分类id查询二级分类 三级分类
     */
    @Override
    public List<CategoryEntity> queryCategoriesWithSubByPid(Long pid) {
        return this.baseMapper.queryCategoriesWithSubByPid(pid);
    }

    /**
     * 根据三级分类的id查询一二三级分类的集合
     */
    @Override
    public List<CategoryEntity> query123categoriesByCid(Long cid) {
        //根据三级分类id查询三级分类信息
        CategoryEntity lv3categoryEntity = this.baseMapper.selectById(cid);
        //根据三级分类的pid作为id查询二级分类信息
        CategoryEntity lv2categoryEntity = this.baseMapper.selectById(lv3categoryEntity.getParentId());
        //根据二级分类pid作为id查询一级分类信息
        CategoryEntity lv1categoryEntity = this.baseMapper.selectById(lv2categoryEntity.getParentId());
        return Arrays.asList(lv1categoryEntity,lv2categoryEntity,lv3categoryEntity);
    }

}