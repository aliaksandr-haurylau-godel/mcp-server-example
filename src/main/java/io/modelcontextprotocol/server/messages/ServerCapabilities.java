package io.modelcontextprotocol.server.messages;

import java.util.List;
import java.util.Map;
// import com.google.gson.JsonElement; // For tool schema if using raw JsonElement

/**
 * Defines the server's capabilities, sent in the InitializeResult.
 */
public class ServerCapabilities {
    // Example: private TextDocumentSyncOptions textDocumentSync;
    private List<ToolDefinition> tools; // List of available tools

    public ServerCapabilities() {
    }

    public List<ToolDefinition> getTools() {
        return tools;
    }

    public void setTools(List<ToolDefinition> tools) {
        this.tools = tools;
    }
}
