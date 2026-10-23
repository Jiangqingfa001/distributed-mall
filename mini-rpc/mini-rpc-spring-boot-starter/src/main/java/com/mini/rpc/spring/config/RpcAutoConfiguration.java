package com.mini.rpc.spring.config;

import com.mini.rpc.core.loadbalance.LoadBalance;
import com.mini.rpc.core.loadbalance.RandomLoadBalance;
import com.mini.rpc.core.loadbalance.RoundRobinLoadBalance;
import com.mini.rpc.core.protocol.RpcProtocol;
import com.mini.rpc.core.registry.ServiceDiscovery;
import com.mini.rpc.core.registry.ServiceRegistry;
import com.mini.rpc.core.registry.ZkServiceDiscovery;
import com.mini.rpc.core.registry.ZkServiceRegistry;
import com.mini.rpc.core.transport.RpcClient;
import com.mini.rpc.core.transport.RpcServer;
import com.mini.rpc.spring.processor.RpcBeanPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RpcServerProperties.class)
public class RpcAutoConfiguration {

    @Bean
    public ServiceRegistry serviceRegistry(RpcServerProperties properties) {
        return new ZkServiceRegistry(properties.getZkAddress());
    }

    @Bean
    public ServiceDiscovery serviceDiscovery(RpcServerProperties properties) {
        return new ZkServiceDiscovery(properties.getZkAddress());
    }

    @Bean
    public RpcServer rpcServer(RpcServerProperties properties, ServiceRegistry serviceRegistry) {
        return new RpcServer(properties.getServerHost(), properties.getServerPort(), serviceRegistry);
    }

    @Bean
    public RpcClient rpcClient() {
        return new RpcClient();
    }

    @Bean
    public LoadBalance loadBalance(RpcServerProperties properties) {
        switch (properties.getLoadBalance()) {
            case "roundRobin":
                return new RoundRobinLoadBalance();
            default:
                return new RandomLoadBalance();
        }
    }

    @Bean
    public byte serializeType(RpcServerProperties properties) {
        switch (properties.getSerializeType()) {
            case "kryo":
                return RpcProtocol.SERIALIZE_KRYO;
            default:
                return RpcProtocol.SERIALIZE_JSON;
        }
    }

    @Bean
    public RpcBeanPostProcessor rpcBeanPostProcessor(RpcServer rpcServer, RpcClient rpcClient,
                                                      ServiceDiscovery serviceDiscovery, LoadBalance loadBalance,
                                                      byte serializeType) {
        return new RpcBeanPostProcessor(rpcServer, rpcClient, serviceDiscovery, loadBalance, serializeType);
    }

    @Bean
    public SmartLifecycle rpcServerStarter(RpcServer rpcServer) {
        return new SmartLifecycle() {
            private boolean running = false;

            @Override
            public void start() {
                try {
                    rpcServer.start();
                    rpcServer.registerServices();
                    running = true;
                } catch (Exception e) {
                    throw new RuntimeException("RPC 服务端启动失败", e);
                }
            }

            @Override
            public void stop() {
                rpcServer.stop();
                running = false;
            }

            @Override
            public boolean isRunning() {
                return running;
            }

            @Override
            public int getPhase() {
                return Integer.MAX_VALUE;
            }
        };
    }
}