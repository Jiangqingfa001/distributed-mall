package com.mini.rpc.core.serialize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

public class SerializerFactory {

    private static final Logger log = LoggerFactory.getLogger(SerializerFactory.class);

    private static final Map<Byte, Serializer> SERIALIZER_MAP = new ConcurrentHashMap<>();

    static { // 静态初始化时加载所有序列化器实现
        // 通过 Java SPI（META-INF/services）加载所有序列化器实现，实现可插拔扩展。
        // 新增序列化器只需：实现 Serializer 接口 + 在 SPI 文件加一行，工厂代码零改动。
        ServiceLoader<Serializer> loader = ServiceLoader.load(Serializer.class);
        for (Serializer serializer : loader) {
            SERIALIZER_MAP.put(serializer.getType(), serializer);
            log.info("通过 SPI 加载序列化器: {} (type={})",
                serializer.getClass().getSimpleName(), serializer.getType());
        }
    }

    public static Serializer getSerializer(byte type) {
        Serializer serializer = SERIALIZER_MAP.get(type);
        if (serializer == null) {
            throw new IllegalArgumentException("不支持的序列化类型: " + type);
        }
        return serializer;
    }
}
