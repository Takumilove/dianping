package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author RaoPengFei
 * @date 2024/6/17
 */
@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        // 配置
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://47.115.207.9:6379")
              .setPassword("1107");
        // 创建RedissonClient对象
        return Redisson.create(config);
    }
    //
    // @Bean
    // public RedissonClient redissonClient2() {
    //     // 配置
    //     Config config = new Config();
    //     config.useSingleServer().setAddress("redis://192.168.142.128:6380").setPassword("1107");
    //     // 创建RedissonClient对象
    //     return Redisson.create(config);
    // }
    //
    // @Bean
    // public RedissonClient redissonClient3() {
    //     // 配置
    //     Config config = new Config();
    //     config.useSingleServer().setAddress("redis://192.168.142.128:6381").setPassword("1107");
    //     // 创建RedissonClient对象
    //     return Redisson.create(config);
    // }
}
