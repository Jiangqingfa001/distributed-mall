package com.mini.rpc.core.proxy;

import com.mini.rpc.core.loadbalance.LoadBalance;
import com.mini.rpc.core.protocol.RpcProtocol;
import com.mini.rpc.core.protocol.RpcRequest;
import com.mini.rpc.core.protocol.RpcResponse;
import com.mini.rpc.core.registry.ServiceDiscovery;
import com.mini.rpc.core.transport.RpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class RpcProxy implements InvocationHandler {

    private static final Logger log = LoggerFactory.getLogger(RpcProxy.class);

    private final ServiceDiscovery serviceDiscovery;
    private final RpcClient rpcClient;
    private final LoadBalance loadBalance;
    private final String version;
    private final byte serializeType;

    public RpcProxy(ServiceDiscovery serviceDiscovery, RpcClient rpcClient,
                    LoadBalance loadBalance, String version, byte serializeType) {
        this.serviceDiscovery = serviceDiscovery;
        this.rpcClient = rpcClient;
        this.loadBalance = loadBalance;
        this.version = version;
        this.serializeType = serializeType;
    }

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> interfaceClass,
                               ServiceDiscovery serviceDiscovery,
                               RpcClient rpcClient,
                               LoadBalance loadBalance,
                               String version,
                               byte serializeType) {
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[]{interfaceClass},
            new RpcProxy(serviceDiscovery, rpcClient, loadBalance, version, serializeType)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        String serviceName = method.getDeclaringClass().getName();

        List<String> addresses = serviceDiscovery.discover(serviceName);
        if (addresses.isEmpty()) {
            throw new RuntimeException("未找到可用服务: " + serviceName);
        }

        String address = loadBalance.select(addresses, serviceName);
        log.debug("选择服务节点: {} -> {}", serviceName, address);

        RpcRequest request = new RpcRequest();
        request.setServiceName(serviceName);
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);
        request.setVersion(version);

        RpcResponse response = rpcClient.send(address, request, serializeType);

        if (!response.isSuccess()) {
            throw new RuntimeException("RPC 调用失败: " + response.getErrorMessage());
        }

        return response.getResult();
    }
}