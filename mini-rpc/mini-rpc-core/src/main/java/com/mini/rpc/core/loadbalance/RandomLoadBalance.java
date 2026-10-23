package com.mini.rpc.core.loadbalance;

import java.util.List;
import java.util.Random;

public class RandomLoadBalance implements LoadBalance {

    private final Random random = new Random();

    @Override
    public String select(List<String> addresses, String serviceName) {
        if (addresses == null || addresses.isEmpty()) {
            throw new RuntimeException("没有可用的服务节点: " + serviceName);
        }
        return addresses.get(random.nextInt(addresses.size()));
    }
}