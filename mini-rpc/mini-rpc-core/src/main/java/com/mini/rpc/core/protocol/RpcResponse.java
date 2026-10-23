package com.mini.rpc.core.protocol;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

public class RpcResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String requestId;
    private Object result;
    private String errorMessage;

    @JsonIgnore
    public boolean isSuccess() {
        return errorMessage == null;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 便捷方法：将异常转为错误消息。
     * 跨 JVM 只传递错误消息字符串，避免 Throwable 序列化/反序列化的复杂性。
     */
    @JsonIgnore
    public void setError(Throwable error) {
        if (error == null) {
            this.errorMessage = null;
            return;
        }
        this.errorMessage = error.getMessage() != null
            ? error.getMessage()
            : error.getClass().getSimpleName();
    }
}
