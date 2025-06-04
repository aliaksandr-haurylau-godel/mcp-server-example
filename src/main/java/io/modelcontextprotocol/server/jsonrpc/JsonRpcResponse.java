package io.modelcontextprotocol.server.jsonrpc;

import com.google.gson.JsonElement; // Placeholder for Gson

/**
 * Represents a JSON-RPC 2.0 Response object.
 */
public class JsonRpcResponse extends JsonRpcMessage {
    private Object id; // Should match the id of the request
    private JsonElement result; // Required on success
    private ErrorObject error;  // Required on error

    // Constructor for success response
    public JsonRpcResponse(Object id, JsonElement result) {
        this.id = id;
        this.result = result;
        this.error = null;
    }

    // Constructor for error response
    public JsonRpcResponse(Object id, ErrorObject error) {
        this.id = id;
        this.result = null;
        this.error = error;
    }

    // Getters
    public Object getId() {
        return id;
    }

    public JsonElement getResult() {
        return result;
    }

    public ErrorObject getError() {
        return error;
    }

    // Setters
    public void setId(Object id) {
        this.id = id;
    }

    public void setResult(JsonElement result) {
        this.result = result;
    }

    public void setError(ErrorObject error) {
        this.error = error;
    }
}
