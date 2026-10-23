package com.mini.rpc.core.loadbalance;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashLoadBalance implements LoadBalance {

    private static final int VIRTUAL_NODE_COUNT = 160;

    private final SortedMap<Integer, String> hashRing = new TreeMap<>();

    public ConsistentHashLoadBalance(List<String> addresses) {
        for (String address : addresses) {
            addNode(address);
        }
    }

    private void addNode(String address) {
        for (int i = 0; i < VIRTUAL_NODE_COUNT; i++) {
            int hash = hash(address + "#VN" + i);
            hashRing.put(hash, address);
        }
    }

    @Override
    public String select(List<String> addresses, String serviceName) {
        if (addresses == null || addresses.isEmpty()) {
            throw new RuntimeException("没有可用的服务节点: " + serviceName);
        }

        int hash = hash(serviceName);
        SortedMap<Integer, String> tailMap = hashRing.tailMap(hash);

        if (tailMap.isEmpty()) {
            return hashRing.get(hashRing.firstKey());
        }
        return tailMap.get(tailMap.firstKey());
    }

    private int hash(String key) {
        int h;
        return (key == null) ? 0 : Math.abs((h = key.hashCode()) ^ (h >>> 16));
    }
}