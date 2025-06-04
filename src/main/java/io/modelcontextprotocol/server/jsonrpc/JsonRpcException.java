package io.modelcontextprotocol.server.jsonrpc;

import com.google.gson.JsonElement;

/**
 * Custom exception for JSON-RPC errors.
 */
public class JsonRpcException extends Exception {
    private final int errorCode;
    private final Object requestId; // Store the request ID for accurate error responses
    private JsonElement errorData;   // Optional data field for the error object

    public JsonRpcException(int errorCode, String message, Object requestId) {
        super(message);
        this.errorCode = errorCode;
        this.requestId = requestId;
    }

    public JsonRpcException(int errorCode, String message, Object requestId, JsonElement errorData) {
        super(message);
        this.errorCode = errorCode;
        this.requestId = requestId;
        this.errorData = errorData;
    }

    public JsonRpcException(int errorCode, String message, Object requestId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.requestId = requestId;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public Object getRequestId() {
        return requestId;
    }

    public JsonElement getErrorData() {
        return errorData;
    }
}
