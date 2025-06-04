package io.modelcontextprotocol.server.messages;

import com.google.gson.JsonElement; // Using JsonElement for schema flexibility

/**
 * Describes a tool provided by the server.
 */
public class ToolDefinition {
    private String name;
    private String description;
    private JsonElement inputSchema;  // Schema for the tool's input parameters
    private JsonElement outputSchema; // Schema for the tool's output

    public ToolDefinition(String name, String description, JsonElement inputSchema, JsonElement outputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
    }

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public JsonElement getInputSchema() { return inputSchema; }
    public JsonElement getOutputSchema() { return outputSchema; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setInputSchema(JsonElement inputSchema) { this.inputSchema = inputSchema; }
    public void setOutputSchema(JsonElement outputSchema) { this.outputSchema = outputSchema; }
}
