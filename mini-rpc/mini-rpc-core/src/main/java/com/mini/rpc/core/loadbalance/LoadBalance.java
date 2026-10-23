package com.mini.rpc.core.loadbalance;

import java.util.List;

public interface LoadBalance {

    String select(List<String> addresses, String serviceName);
}