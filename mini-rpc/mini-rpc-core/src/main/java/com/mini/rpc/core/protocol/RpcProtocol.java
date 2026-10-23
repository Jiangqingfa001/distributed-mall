package com.mini.rpc.core.protocol;

public class RpcProtocol {

    public static final short MAGIC = (short) 0xDADA;// 协议魔数

    public static final byte VERSION = 1;

    public static final int HEADER_LENGTH = 10;

    public static final byte TYPE_REQUEST = 1;
    public static final byte TYPE_RESPONSE = 2;
    public static final byte TYPE_HEARTBEAT_REQUEST = 3;
    public static final byte TYPE_HEARTBEAT_RESPONSE = 4;

    public static final byte SERIALIZE_JSON = 1;
    public static final byte SERIALIZE_KRYO = 3;

    public static final byte STATUS_SUCCESS = 0;
    public static final byte STATUS_FAIL = 1;
}