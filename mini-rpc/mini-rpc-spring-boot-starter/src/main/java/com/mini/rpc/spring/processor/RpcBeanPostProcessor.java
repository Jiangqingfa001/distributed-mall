package com.mini.rpc.spring.processor;

import com.mini.rpc.core.loadbalance.LoadBalance;
import com.mini.rpc.core.registry.ServiceDiscovery;
import com.mini.rpc.core.transport.RpcClient;
import com.mini.rpc.core.transport.RpcServer;
import com.mini.rpc.core.proxy.RpcProxy;
import com.mini.rpc.spring.annotation.RpcReference;
import com.mini.rpc.spring.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;

public class RpcBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(RpcBeanPostProcessor.class);

    private final RpcServer rpcServer;
    private final RpcClient rpcClient;
    private final ServiceDiscovery serviceDiscovery;
    private final LoadBalance loadBalance;
    private final byte serializeType;

    public RpcBeanPostProcessor(RpcServer rpcServer, RpcClient rpcClient,
                                ServiceDiscovery serviceDiscovery, LoadBalance loadBalance,
                                byte serializeType) {
        this.rpcServer = rpcServer;
        this.rpcClient = rpcClient;
        this.serviceDiscovery = serviceDiscovery;
        this.loadBalance = loadBalance;
        this.serializeType = serializeType;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();

        RpcService rpcService = beanClass.getAnnotation(RpcService.class);
        if (rpcService != null) {
            registerServiceProvider(bean, beanClass, rpcService);
        }

        for (Field field : beanClass.getDeclaredFields()) {
            RpcReference rpcReference = field.getAnnotation(RpcReference.class);
            if (rpcReference != null) {
                injectServiceConsumer(bean, field, rpcReference);
            }
        }

        return bean;
    }

    private void registerServiceProvider(Object bean, Class<?> beanClass, RpcService rpcService) {
        Class<?> interfaceClass = rpcService.interfaceClass();
        if (interfaceClass == void.class) {
            Class<?>[] interfaces = beanClass.getInterfaces();
            if (interfaces.length == 0) {
                throw new RuntimeException("@RpcService 标注的类必须实现接口: " + beanClass.getName());
            }
            interfaceClass = interfaces[0];
        }

        String serviceName = interfaceClass.getName();
        rpcServer.addService(serviceName, bean);
        log.info("注册 RPC 服务: {} -> {}", serviceName, beanClass.getSimpleName());
    }

    private void injectServiceConsumer(Object bean, Field field, RpcReference rpcReference) {
        Class<?> interfaceClass = field.getType();
        if (!interfaceClass.isInterface()) {
            throw new RuntimeException("@RpcReference 只能标注接口类型: " + field.getName());
        }

        Object proxy = RpcProxy.create(
            interfaceClass,
            serviceDiscovery,
            rpcClient,
            loadBalance,
            rpcReference.version(),
            serializeType
        );

        field.setAccessible(true);
        try {
            field.set(bean, proxy);
            log.info("注入 RPC 代理: {} -> {}", interfaceClass.getSimpleName(), bean.getClass().getSimpleName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("注入 RPC 代理失败: " + field.getName(), e);
        }
    }
}