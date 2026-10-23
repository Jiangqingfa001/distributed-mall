package com.mini.rpc.core.registry;

import java.util.List;
import java.util.function.Consumer;

public interface ServiceDiscovery {

    List<String> discover(String serviceName) throws Exception;

    void watch(String serviceName, Consumer<List<String>> callback) throws Exception;
}