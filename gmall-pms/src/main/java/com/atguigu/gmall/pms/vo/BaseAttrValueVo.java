package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Data
public class BaseAttrValueVo extends SpuAttrValueEntity {

    // 赋值给SpuAttrValueEntity表中的AttrValue 的方法
    private void setValueSelected(List<String> valueSelected) {

        if (!CollectionUtils.isEmpty(valueSelected)) {
            // 因为AttrValue是string型,
            // list转换字符串
            this.setAttrValue(StringUtils.join(valueSelected,","));
        }
    }

}
