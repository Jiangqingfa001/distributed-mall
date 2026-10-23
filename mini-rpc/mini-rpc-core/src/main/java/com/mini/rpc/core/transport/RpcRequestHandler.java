package com.mini.rpc.core.transport;

import com.mini.rpc.core.codec.RpcDecoder;
import com.mini.rpc.core.codec.RpcEncoder;
import com.mini.rpc.core.protocol.RpcMessage;
import com.mini.rpc.core.protocol.RpcProtocol;
import com.mini.rpc.core.protocol.RpcRequest;
import com.mini.rpc.core.protocol.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpcRequestHandler extends SimpleChannelInboundHandler<RpcMessage> {

    private static final Logger log = LoggerFactory.getLogger(RpcRequestHandler.class);

    private final Map<String, Object> serviceMap;

    public RpcRequestHandler() {
        this.serviceMap = new ConcurrentHashMap<>();
    }

    public RpcRequestHandler(Map<String, Object> sharedServiceMap) {
        this.serviceMap = sharedServiceMap;
    }

    public void addService(String serviceName, Object serviceBean) {
        serviceMap.put(serviceName, serviceBean);
        log.info("注册本地服务: {}", serviceName);
    }

    public Map<String, Object> getServiceMap() {
        return serviceMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) throws Exception {
        if (msg.getMessageType() != RpcProtocol.TYPE_REQUEST) {
            return;
        }

        RpcRequest request = (RpcRequest) msg.getBody();
        RpcResponse response = new RpcResponse();
        response.setRequestId(request.getRequestId());

        try {
            Object result = handleRequest(request);
            response.setResult(result);
        } catch (Exception e) {
            log.error("RPC 调用失败: {}", e.getMessage(), e);
            response.setError(e);
        }

        RpcMessage responseMsg = new RpcMessage();
        responseMsg.setMagic(RpcProtocol.MAGIC);
        responseMsg.setVersion(RpcProtocol.VERSION);
        responseMsg.setSerializeType(msg.getSerializeType());
        responseMsg.setMessageType(RpcProtocol.TYPE_RESPONSE);
        responseMsg.setStatus(response.isSuccess() ? RpcProtocol.STATUS_SUCCESS : RpcProtocol.STATUS_FAIL);
        responseMsg.setBody(response);

        ctx.writeAndFlush(responseMsg);
    }

    private Object handleRequest(RpcRequest request) throws Exception {
        String serviceName = request.getServiceName();
        Object serviceBean = serviceMap.get(serviceName);

        if (serviceBean == null) {
            throw new RuntimeException("未找到服务: " + serviceName);
        }

        Method method = serviceBean.getClass().getMethod(
            request.getMethodName(),
            request.getParameterTypes()
        );

        Object[] parameters = request.getParameters();
        Class<?>[] parameterTypes = request.getParameterTypes();
        if (parameters != null && parameterTypes != null) {
            for (int i = 0; i < parameters.length && i < parameterTypes.length; i++) {
                parameters[i] = convertParameter(parameters[i], parameterTypes[i]);
            }
        }

        try {
            return method.invoke(serviceBean, parameters);
        } catch (InvocationTargetException e) {
            // 解包反射包装异常，保留原始业务异常信息（如"用户不存在"、"库存不足"）
            Throwable cause = e.getTargetException();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause.getMessage(), cause);
        }
    }

    private Object convertParameter(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        Class<?> sourceType = value.getClass();
        if (targetType.isAssignableFrom(sourceType)) {
            return value;
        }
        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.valueOf(value.toString());
        }
        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.valueOf(value.toString());
        }
        if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.valueOf(value.toString());
        }
        if (targetType == Float.class || targetType == float.class) {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return Float.valueOf(value.toString());
        }
        if (targetType == Short.class || targetType == short.class) {
            if (value instanceof Number) {
                return ((Number) value).shortValue();
            }
            return Short.valueOf(value.toString());
        }
        if (targetType == Byte.class || targetType == byte.class) {
            if (value instanceof Number) {
                return ((Number) value).byteValue();
            }
            return Byte.valueOf(value.toString());
        }
        if (targetType == String.class) {
            return value.toString();
        }
        return value;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("RpcRequestHandler 异常: {}", cause.getMessage(), cause);
        ctx.close();
    }
}