package com.mini.rpc.core.transport;

import com.mini.rpc.core.codec.RpcDecoder;
import com.mini.rpc.core.codec.RpcEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private final Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    public ConnectionManager() {
        this.group = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(group)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new RpcEncoder());
                    pipeline.addLast(new RpcDecoder());
                    pipeline.addLast(new RpcResponseHandler());
                }
            });
    }

    public Channel getChannel(String address) throws Exception {
        Channel channel = channelMap.get(address);
        if (channel == null || !channel.isActive()) {
            synchronized (this) {
                channel = channelMap.get(address);
                if (channel == null || !channel.isActive()) {
                    String[] parts = address.split(":");
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);

                    ChannelFuture future = bootstrap.connect(host, port).sync();
                    channel = future.channel();
                    channelMap.put(address, channel);
                    log.info("建立连接: {}", address);
                }
            }
        }
        return channel;
    }

    public void close() {
        channelMap.values().forEach(ch -> {
            if (ch.isActive()) {
                ch.close();
            }
        });
        group.shutdownGracefully();
        log.info("连接管理器已关闭");
    }
}