package com.atguigu.gmall.pms.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.zaxxer.hikari.HikariDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
/*
* 注入数据源
Seata 通过代理数据源的方式实现分支事务
* */
@Configuration
public class DataSourceProxyConfig {
//    因为有两个启动会报错 报数据源循环 ,所以只留下一个
//    @Bean
//    @ConfigurationProperties(prefix = "spring.datasource")
//    public DataSource dataSource(@Value("${spring.datasource.url}") String url) {
//        //将德鲁伊数据源 改为 HikariDataSource
//        //HikariDataSource 中是jdbcurl  不是url,所以不能直接从配置文件中赋值,需要手动赋值
//        HikariDataSource hikariDataSource = new HikariDataSource();
//        hikariDataSource.setJdbcUrl(url);
//
//        return hikariDataSource;
//    }

    /**
     * 需要将DataSourceProxy 设置为主数据源 , 否则事务无法回滚
     */
    @Bean
    @Primary  // 代表是主数据源
    public DataSourceProxy dataSourceProxy(@Value("${spring.datasource.url}") String url,
                                           @Value("${spring.datasource.driver-class-name}") String driverClass,
                                           @Value("${spring.datasource.username}") String username,
                                           @Value("${spring.datasource.password}") String password) {
        //将德鲁伊数据源 改为 HikariDataSource
        //HikariDataSource 中是jdbcurl  不是url,所以不能直接从配置文件中赋值,需要手动赋值
        //注意:如果只剩下一个方法,因为这个方法是代理数据源,所以需要手动赋四个值
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setJdbcUrl(url);
        hikariDataSource.setDriverClassName(driverClass);
        hikariDataSource.setUsername(username);
        hikariDataSource.setPassword(password);
        return new DataSourceProxy(hikariDataSource);
    }

//    如果删除下面这些 上面需要指定主数据源
//    @Bean
//    public SqlSessionFactory sqlSessionFactoryBean(DataSourceProxy dataSourceProxy) throws Exception {
//        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
//        sqlSessionFactoryBean.setDataSource(dataSourceProxy);
//        return sqlSessionFactoryBean.getObject();
//    }
}