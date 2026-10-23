package com.mini.rpc.core.heartbeat;

import com.mini.rpc.core.protocol.RpcMessage;
import com.mini.rpc.core.protocol.RpcProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class HeartBeatHandler extends SimpleChannelInboundHandler<RpcMessage> {

    private static final Logger log = LoggerFactory.getLogger(HeartBeatHandler.class);

    public static final int READER_IDLE_TIME = 30;
    public static final int WRITER_IDLE_TIME = 0;
    public static final int ALL_IDLE_TIME = 0;

    public static IdleStateHandler createIdleStateHandler() {
        return new IdleStateHandler(READER_IDLE_TIME, WRITER_IDLE_TIME, ALL_IDLE_TIME, TimeUnit.SECONDS);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {
        if (msg.getMessageType() == RpcProtocol.TYPE_HEARTBEAT_REQUEST) {
            log.debug("收到心跳请求，发送心跳响应");
            RpcMessage pong = new RpcMessage();
            pong.setMagic(RpcProtocol.MAGIC);
            pong.setVersion(RpcProtocol.VERSION);
            pong.setSerializeType(RpcProtocol.SERIALIZE_JSON);
            pong.setMessageType(RpcProtocol.TYPE_HEARTBEAT_RESPONSE);
            pong.setStatus(RpcProtocol.STATUS_SUCCESS);
            ctx.writeAndFlush(pong);
        }

        if (msg.getMessageType() == RpcProtocol.TYPE_HEARTBEAT_RESPONSE) {
            log.debug("收到心跳响应");
        }

        ctx.fireChannelRead(msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.warn("读空闲超时，发送心跳请求");
                RpcMessage ping = new RpcMessage();
                ping.setMagic(RpcProtocol.MAGIC);
                ping.setVersion(RpcProtocol.VERSION);
                ping.setSerializeType(RpcProtocol.SERIALIZE_JSON);
                ping.setMessageType(RpcProtocol.TYPE_HEARTBEAT_REQUEST);
                ping.setStatus(RpcProtocol.STATUS_SUCCESS);
                ctx.writeAndFlush(ping);
            }
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("心跳检测异常: {}", cause.getMessage());
        ctx.close();
    }
}