package com.mini.rpc.core.transport;

import com.mini.rpc.core.codec.RpcDecoder;
import com.mini.rpc.core.codec.RpcEncoder;
import com.mini.rpc.core.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RpcServer {

    private static final Logger log = LoggerFactory.getLogger(RpcServer.class);

    private final String host;
    private final int port;
    private final ServiceRegistry serviceRegistry;
    private final RpcRequestHandler requestHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean started = false;

    public RpcServer(String host, int port, ServiceRegistry serviceRegistry) {
        this.host = host;
        this.port = port;
        this.serviceRegistry = serviceRegistry;
        this.requestHandler = new RpcRequestHandler();
    }

    public void addService(String serviceName, Object serviceBean) {
        requestHandler.addService(serviceName, serviceBean);
    }

    public Map<String, Object> getServiceMap() {
        return requestHandler.getServiceMap();
    }

    public void start() throws Exception {
        if (started) {
            return;
        }
        started = true;
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new RpcEncoder());
                    pipeline.addLast(new RpcDecoder());
                    pipeline.addLast(new RpcRequestHandler(requestHandler.getServiceMap()));
                }
            });

        ChannelFuture future = bootstrap.bind(host, port).sync();
        serverChannel = future.channel();

        String address = host + ":" + port;
        log.info("RPC 服务端启动成功: {}", address);
    }

    public void registerServices() throws Exception {
        String address = host + ":" + port;
        Map<String, Object> services = requestHandler.getServiceMap();
        log.info("开始注册服务, 共 {} 个服务: {}", services.size(), services.keySet());
        for (String serviceName : services.keySet()) {
            serviceRegistry.register(serviceName, address);
            log.info("已注册服务到 ZK: {} -> {}", serviceName, address);
        }
    }

    public void stop() {
        try {
            String address = host + ":" + port;
            for (String serviceName : requestHandler.getServiceMap().keySet()) {
                serviceRegistry.unregister(serviceName, address);
            }
        } catch (Exception e) {
            log.error("注销服务失败", e);
        }

        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("RPC 服务端已停止");
    }
}