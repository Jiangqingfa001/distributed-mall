package com.mini.rpc.core.registry;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class ZkServiceRegistry implements ServiceRegistry {

    private static final Logger log = LoggerFactory.getLogger(ZkServiceRegistry.class);

    private static final String BASE_PATH = "/mini-rpc";

    private final CuratorFramework client;

    public ZkServiceRegistry(String zkAddress) {
        this.client = CuratorFrameworkFactory.builder()
            .connectString(zkAddress)
            .sessionTimeoutMs(30000)
            .connectionTimeoutMs(15000)
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .build();
        this.client.start();
        log.info("Zookeeper 注册中心已连接: {}", zkAddress);
    }

    @Override
    public void register(String serviceName, String address) throws Exception {
        String path = BASE_PATH + "/" + serviceName + "/" + address;

        // EPHEMERAL 节点只属于创建它的 session。ZK 重启后旧临时节点可能仍残留，
        // 导致 checkExists 返回 true 而跳过注册；旧 session 过期后节点被删除，服务实际未注册。
        // 因此这里先删除残留的旧节点，再以当前 session 重新创建。
        if (client.checkExists().forPath(path) != null) {
            client.delete().forPath(path);
            log.info("删除残留的旧节点: {}", path);
        }
        client.create()
            .creatingParentsIfNeeded()
            .withMode(CreateMode.EPHEMERAL)
            .forPath(path);
        log.info("ZK 路径创建成功: {}", path);
    }

    @Override
    public void unregister(String serviceName, String address) throws Exception {
        String path = BASE_PATH + "/" + serviceName + "/" + address;

        if (client.checkExists().forPath(path) != null) {
            client.delete().forPath(path);
            log.info("服务注销成功: {} -> {}", serviceName, address);
        }
    }

    @Override
    public List<String> discover(String serviceName) throws Exception {
        String path = BASE_PATH + "/" + serviceName;

        if (client.checkExists().forPath(path) == null) {
            return Collections.emptyList();
        }

        return client.getChildren().forPath(path);
    }

    public void close() {
        if (client != null) {
            client.close();
            log.info("Zookeeper 注册中心已关闭");
        }
    }
}