package io.modelcontextprotocol.server.jsonrpc;

import com.google.gson.JsonElement; // Placeholder for Gson

/**
 * Represents the "error" object in a JSON-RPC 2.0 Response.
 */
public class ErrorObject {
    private int code;
    private String message;
    private JsonElement data; // Optional

    // Constructors
    public ErrorObject(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public ErrorObject(int code, String message, JsonElement data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // Getters
    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public JsonElement getData() {
        return data;
    }

    // Setters
    public void setCode(int code) {
        this.code = code;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(JsonElement data) {
        this.data = data;
    }
}
