package com.mini.rpc.core.transport;

import com.mini.rpc.core.protocol.RpcResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RpcFutureManager {

    private static final ConcurrentMap<String, CompletableFuture<RpcResponse>> FUTURE_MAP = new ConcurrentHashMap<>();

    public static void put(String requestId, CompletableFuture<RpcResponse> future) {
        FUTURE_MAP.put(requestId, future);
    }

    public static CompletableFuture<RpcResponse> get(String requestId) {
        return FUTURE_MAP.get(requestId);
    }

    public static void remove(String requestId) {
        FUTURE_MAP.remove(requestId);
    }
}