package com.mini.rpc.core.loadbalance;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalance implements LoadBalance {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public String select(List<String> addresses, String serviceName) {
        if (addresses == null || addresses.isEmpty()) {
            throw new RuntimeException("没有可用的服务节点: " + serviceName);
        }
        int index = counter.getAndIncrement() % addresses.size();
        return addresses.get(index);
    }
}