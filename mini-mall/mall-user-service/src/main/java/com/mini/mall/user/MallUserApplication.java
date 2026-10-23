package com.mini.mall.user;

import com.mini.rpc.core.transport.RpcServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class MallUserApplication {

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext context = SpringApplication.run(MallUserApplication.class, args);

        RpcServer rpcServer = context.getBean(RpcServer.class);
        rpcServer.start();
        rpcServer.registerServices();

        System.out.println("========================================");
        System.out.println("  用户服务启动成功!");
        System.out.println("========================================");
    }
}