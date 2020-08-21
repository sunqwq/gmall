package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class demo01 {
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuAttrValueService skuAttrValueService;

    //测试 根据spuId查询spu下所有sku的销售属性attr_value与skuId之间的映射关系

    @Test
    public void test01(){
        List<Map<String, Object>> maps = this.skuAttrValueMapper.querySaleAttrMappingSkuIdBySpuId(20l);
        System.out.println("maps = " + maps);

        String s = this.skuAttrValueService.querySaleAttrMappingSkuIdBySpuId(20l);
        System.out.println("s = " + s);

        /**
         *maps = [{sku_id=19, attr_values=金色,6G,128G}, {sku_id=20, attr_values=256G,6G,金色}, {sku_id=21, attr_values=金色,6G,512G}, {sku_id=22, attr_values=金色,12G,128G}, {sku_id=23, attr_values=256G,12G,金色}, {sku_id=24, attr_values=金色,12G,512G}, {sku_id=25, attr_values=白色,6G,128G}, {sku_id=26, attr_values=256G,6G,白色}, {sku_id=27, attr_values=白色,6G,512G}, {sku_id=28, attr_values=128G,12G,白色}, {sku_id=29, attr_values=白色,12G,256G}, {sku_id=30, attr_values=512G,12G,白色}]
         * s = {"金色,6G,512G":21,"256G,6G,白色":26,"白色,6G,128G":25,"白色,12G,256G":29,"256G,12G,金色":23,"256G,6G,金色":20,"金色,12G,128G":22,"白色,6G,512G":27,"128G,12G,白色":28,"512G,12G,白色":30,"金色,12G,512G":24,"金色,6G,128G":19}
         */
    }

}
