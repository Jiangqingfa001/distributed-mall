package com.mini.rpc.core.transport;

import com.mini.rpc.core.protocol.RpcMessage;
import com.mini.rpc.core.protocol.RpcProtocol;
import com.mini.rpc.core.protocol.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class RpcResponseHandler extends SimpleChannelInboundHandler<RpcMessage> {

    private static final Logger log = LoggerFactory.getLogger(RpcResponseHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {
        if (msg.getMessageType() != RpcProtocol.TYPE_RESPONSE) {
            return;
        }

        RpcResponse response = (RpcResponse) msg.getBody();
        String requestId = response.getRequestId();

        CompletableFuture<RpcResponse> future = RpcFutureManager.get(requestId);
        if (future != null) {
            // 无论成功或失败都 complete，由 RpcProxy 根据 isSuccess() 统一处理
            future.complete(response);
            RpcFutureManager.remove(requestId);
        } else {
            log.warn("未找到对应的 Future: {}", requestId);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("RpcResponseHandler 异常: {}", cause.getMessage(), cause);
        ctx.close();
    }
}