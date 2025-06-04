package io.modelcontextprotocol.server.messages;

import com.google.gson.JsonObject; // Using JsonObject for params flexibility

/**
 * Parameters for a "tool/call" request.
 */
public class ToolCallParams {
    private String toolName;
    private JsonObject params; // The actual parameters for the tool

    public ToolCallParams() {
    }

    public ToolCallParams(String toolName, JsonObject params) {
        this.toolName = toolName;
        this.params = params;
    }

    // Getters
    public String getToolName() {
        return toolName;
    }

    public JsonObject getParams() {
        return params;
    }

    // Setters
    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public void setParams(JsonObject params) {
        this.params = params;
    }
}
