package com.mini.rpc.core.codec;

import com.mini.rpc.core.protocol.RpcMessage;
import com.mini.rpc.core.serialize.Serializer;
import com.mini.rpc.core.serialize.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RpcEncoder extends MessageToByteEncoder<RpcMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage msg, ByteBuf out) throws Exception {
        out.writeShort(msg.getMagic());
        out.writeByte(msg.getVersion());
        out.writeByte(msg.getSerializeType());
        out.writeByte(msg.getMessageType());
        out.writeByte(msg.getStatus());

        Serializer serializer = SerializerFactory.getSerializer(msg.getSerializeType());
        byte[] bodyBytes = serializer.serialize(msg.getBody());

        out.writeInt(bodyBytes.length);
        out.writeBytes(bodyBytes);
    }
}