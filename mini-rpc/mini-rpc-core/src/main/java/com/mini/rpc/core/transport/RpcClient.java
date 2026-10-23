package com.mini.rpc.core.transport;

import com.mini.rpc.core.protocol.RpcMessage;
import com.mini.rpc.core.protocol.RpcProtocol;
import com.mini.rpc.core.protocol.RpcRequest;
import com.mini.rpc.core.protocol.RpcResponse;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RpcClient {

    private static final Logger log = LoggerFactory.getLogger(RpcClient.class);

    private static final long RPC_TIMEOUT_SECONDS = 10;

    // 超时重试：最多重试 2 次（共 3 次尝试），指数退避基数 100ms
    private static final int MAX_RETRIES = 2;
    private static final long BASE_BACKOFF_MS = 100;

    private final ConnectionManager connectionManager;

    public RpcClient() {
        this.connectionManager = new ConnectionManager();
    }

    /**
     * 发送 RPC 请求，带超时重试（指数退避）。
     * 只重试「超时」这类网络层不确定异常：请求可能已送达但响应丢失，此时重试相对安全。
     * 业务失败（RpcResponse 携带 errorMessage）不在此重试，由调用方（RpcProxy）直接抛错，
     * 避免对非幂等操作重复执行、产生副作用。
     */
    public RpcResponse send(String address, RpcRequest request, byte serializeType) throws Exception {
        int attempt = 0;
        while (true) {
            try {
                return doSend(address, request, serializeType);
            } catch (TimeoutException e) {
                if (attempt >= MAX_RETRIES) {
                    throw new RuntimeException(
                        "RPC 调用超时（已重试 " + MAX_RETRIES + " 次）: "
                            + request.getServiceName() + "." + request.getMethodName() + " -> " + address, e);
                }
                long backoff = BASE_BACKOFF_MS * (1L << attempt);
                log.warn("RPC 调用超时，{}ms 后第 {} 次重试: {}.{} -> {}",
                    backoff, attempt + 1, request.getServiceName(), request.getMethodName(), address);
                Thread.sleep(backoff);
                attempt++;
            }
        }
    }

    private RpcResponse doSend(String address, RpcRequest request, byte serializeType) throws Exception {
        Channel channel = connectionManager.getChannel(address);

        // 每次尝试都生成新的 requestId，避免与上一次超时残留的 future 冲突
        String requestId = UUID.randomUUID().toString();
        request.setRequestId(requestId);

        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        RpcFutureManager.put(requestId, future);

        RpcMessage message = new RpcMessage();
        message.setMagic(RpcProtocol.MAGIC);
        message.setVersion(RpcProtocol.VERSION);
        message.setSerializeType(serializeType);
        message.setMessageType(RpcProtocol.TYPE_REQUEST);
        message.setStatus(RpcProtocol.STATUS_SUCCESS);
        message.setBody(request);

        channel.writeAndFlush(message);

        try {
            return future.get(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } finally {
            // 无论成功、超时还是异常，都要清理 future，避免内存泄漏
            RpcFutureManager.remove(requestId);
        }
    }

    public void close() {
        connectionManager.close();
        log.info("RPC 客户端已关闭");
    }
}
