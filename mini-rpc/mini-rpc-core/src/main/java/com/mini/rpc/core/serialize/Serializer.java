package com.mini.rpc.core.serialize;

public interface Serializer {

    byte[] serialize(Object obj) throws Exception;

    <T> T deserialize(byte[] bytes, Class<T> clazz) throws Exception;

    byte getType();
}