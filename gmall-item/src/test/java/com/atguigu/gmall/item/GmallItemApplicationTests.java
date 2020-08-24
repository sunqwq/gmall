package com.atguigu.gmall.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GmallItemApplicationTests {
    /**
     * 从配置文件中注入 : 两种方式
     * 方式一:
     *      @Value("${threadPool.coreSize}")
     *      Integer coreSize;
     * 方式二:
     */

    @Test
    void contextLoads( @Value("${threadPool.coreSize}")Integer coreSize) {
        System.out.println("coreSize = " + coreSize);
    }

}
