package com.mini.mall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MallGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallGatewayApplication.class, args);
        System.out.println("========================================");
        System.out.println("  API 网关启动成功! http://localhost:8080");
        System.out.println("========================================");
    }
}