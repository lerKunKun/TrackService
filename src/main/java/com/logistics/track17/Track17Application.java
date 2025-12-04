package com.logistics.track17;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Track-17 Logistics System Application
 *
 * @author Jax
 * @version 1.0.0
 */
@SpringBootApplication
@MapperScan("com.logistics.track17.mapper")
@EnableAsync
@EnableScheduling
public class Track17Application {

    public static void main(String[] args) {
        SpringApplication.run(Track17Application.class, args);
    }
}
