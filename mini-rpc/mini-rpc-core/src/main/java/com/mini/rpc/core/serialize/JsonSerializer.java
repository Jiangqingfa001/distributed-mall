package com.mini.rpc.core.serialize;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.mini.rpc.core.protocol.RpcProtocol;

public class JsonSerializer implements Serializer {

    private final ObjectMapper objectMapper;

    public JsonSerializer() {
        this.objectMapper = new ObjectMapper();
        // 序列化时保留具体类型信息（@class 字段），
        // 解决 Object 类型字段（如 RpcResponse.result）反序列化后变成 LinkedHashMap、
        // 无法还原为 User/Product/Order 等具体类型的问题。
        this.objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
    }

    @Override
    public byte[] serialize(Object obj) throws Exception {
        return objectMapper.writeValueAsBytes(obj);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws Exception {
        return objectMapper.readValue(bytes, clazz);
    }

    @Override
    public byte getType() {
        return RpcProtocol.SERIALIZE_JSON;
    }
}
