package com.example.musinsaPointSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 포인트 시스템 메인 애플리케이션
 */
@SpringBootApplication
@EnableConfigurationProperties
public class MssPointSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(MssPointSystemApplication.class, args);
    }
}