package io.modelcontextprotocol.server.jsonrpc;

import com.google.gson.JsonElement; // Placeholder for Gson

/**
 * Represents a JSON-RPC 2.0 Request object.
 */
public class JsonRpcRequest extends JsonRpcMessage {
    private Object id; // Can be String, Number, or null for notifications
    private String method;
    private JsonElement params; // Using JsonElement for flexibility with Gson

    // Constructors
    public JsonRpcRequest(String method, JsonElement params, Object id) {
        this.method = method;
        this.params = params;
        this.id = id;
    }

    public JsonRpcRequest(String method, JsonElement params) {
        this(method, params, null); // For notifications
    }

    // Getters
    public Object getId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    public JsonElement getParams() {
        return params;
    }

    // Setters (useful for deserialization if not using field access)
    public void setId(Object id) {
        this.id = id;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setParams(JsonElement params) {
        this.params = params;
    }

    /**
     * Checks if the request is a notification.
     * @return true if the id is null, false otherwise.
     */
    public boolean isNotification() {
        return this.id == null;
    }
}
