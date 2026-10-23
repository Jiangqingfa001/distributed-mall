package com.mini.rpc.core.serialize;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.mini.rpc.core.protocol.RpcProtocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class KryoSerializer implements Serializer {

    // Kryo 实例非线程安全，且创建开销较大，使用 ThreadLocal 为每个线程池化一个实例
    private static final ThreadLocal<Kryo> KRYO_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false); // 允许未显式注册的类自动序列化
        kryo.setReferences(true);            // 处理对象引用/循环引用
        return kryo;
    });

    @Override
    public byte[] serialize(Object obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Output output = new Output(bos);
        // writeClassAndObject 会写入运行时类型信息，反序列化时可还原具体类型
        //（等价于 JsonSerializer 的 default typing，解决 RpcResponse.result 的类型擦除问题）
        KRYO_THREAD_LOCAL.get().writeClassAndObject(output, obj);
        output.close();
        return bos.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws Exception {
        Input input = new Input(new ByteArrayInputStream(bytes));
        Object obj = KRYO_THREAD_LOCAL.get().readClassAndObject(input);
        input.close();
        return (T) obj;
    }

    @Override
    public byte getType() {
        return RpcProtocol.SERIALIZE_KRYO;
    }
}
