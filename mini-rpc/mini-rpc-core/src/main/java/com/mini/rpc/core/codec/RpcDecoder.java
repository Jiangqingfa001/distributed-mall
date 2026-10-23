package com.mini.rpc.core.codec;

import com.mini.rpc.core.protocol.RpcMessage;
import com.mini.rpc.core.protocol.RpcProtocol;
import com.mini.rpc.core.protocol.RpcRequest;
import com.mini.rpc.core.protocol.RpcResponse;
import com.mini.rpc.core.serialize.Serializer;
import com.mini.rpc.core.serialize.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class RpcDecoder extends LengthFieldBasedFrameDecoder {

    public RpcDecoder() {
        super(
            Integer.MAX_VALUE,
            6,
            4,
            0,
            0
        );
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        RpcMessage message = new RpcMessage();
        message.setMagic(frame.readShort());
        message.setVersion(frame.readByte());
        message.setSerializeType(frame.readByte());
        message.setMessageType(frame.readByte());
        message.setStatus(frame.readByte());

        int bodyLength = frame.readInt();
        message.setBodyLength(bodyLength);

        if (bodyLength > 0) {
            byte[] bodyBytes = new byte[bodyLength];
            frame.readBytes(bodyBytes);

            Serializer serializer = SerializerFactory.getSerializer(message.getSerializeType());
            Class<?> bodyClass = (message.getMessageType() == RpcProtocol.TYPE_REQUEST)
                ? RpcRequest.class : RpcResponse.class;
            message.setBody(serializer.deserialize(bodyBytes, bodyClass));
        }

        frame.release();
        return message;
    }
}