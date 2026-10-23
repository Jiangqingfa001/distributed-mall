package com.mini.rpc.core.registry;

import java.util.List;

public interface ServiceRegistry {

    void register(String serviceName, String address) throws Exception;

    void unregister(String serviceName, String address) throws Exception;

    List<String> discover(String serviceName) throws Exception;
}