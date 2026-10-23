package com.mini.rpc.core.registry;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ZkServiceDiscovery implements ServiceDiscovery {

    private static final Logger log = LoggerFactory.getLogger(ZkServiceDiscovery.class);

    private static final String BASE_PATH = "/mini-rpc";

    private final CuratorFramework client;

    public ZkServiceDiscovery(String zkAddress) {
        this.client = CuratorFrameworkFactory.builder()
            .connectString(zkAddress)
            .sessionTimeoutMs(30000)
            .connectionTimeoutMs(15000)
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .build();
        this.client.start();
        log.info("Zookeeper 服务发现已连接: {}", zkAddress);
    }

    @Override
    public List<String> discover(String serviceName) throws Exception {
        String path = BASE_PATH + "/" + serviceName;

        if (client.checkExists().forPath(path) == null) {
            return Collections.emptyList();
        }

        return client.getChildren().forPath(path);
    }

    @Override
    public void watch(String serviceName, Consumer<List<String>> callback) throws Exception {
        String path = BASE_PATH + "/" + serviceName;

        PathChildrenCache cache = new PathChildrenCache(client, path, true);
        cache.getListenable().addListener((curatorClient, event) -> {
            List<String> addresses = event.getInitialData().stream()
                .map(ChildData::getPath)
                .map(p -> p.substring(p.lastIndexOf('/') + 1))
                .collect(Collectors.toList());

            log.info("服务列表变更 [{}]: {}", serviceName, addresses);
            callback.accept(addresses);
        });
        cache.start();
    }

    public void close() {
        if (client != null) {
            client.close();
            log.info("Zookeeper 服务发现已关闭");
        }
    }
}