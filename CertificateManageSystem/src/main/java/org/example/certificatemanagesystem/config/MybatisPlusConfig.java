package org.example.certificatemanagesystem.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// MyBatis-Plus 插件配置类：没有本类，selectPage() 不会真正分页（不加 LIMIT、total 恒为 0）
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器链（以 Bean 形式交给 Spring 管理，启动时自动装配）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // MybatisPlusInterceptor 是"总拦截器"，内部可挂多个"内部插件"（InnerInterceptor）
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 挂载分页插件：拦截 selectPage 的 SQL，先发 COUNT 查询算 total，再改写为 LIMIT 偏移量,条数
        // DbType.MYSQL 指定按 MySQL 方言生成分页语句（换数据库只需改这里）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
