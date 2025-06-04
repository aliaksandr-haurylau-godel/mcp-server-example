package io.modelcontextprotocol.server;

import io.modelcontextprotocol.server.jsonrpc.*;
import io.modelcontextprotocol.server.messages.*;
import io.modelcontextprotocol.server.tools.EchoTool; // Will be created in next step
import io.modelcontextprotocol.server.transport.StdioTransport;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject; // For creating schema
import com.google.gson.JsonSyntaxException; // For parsing tool params

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class McpServer {
    private static final Logger LOGGER = Logger.getLogger(McpServer.class.getName());
    private final StdioTransport transport;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private ServerCapabilities serverCapabilities;

    // Placeholder for the actual tool instance
    private EchoTool echoTool; // Will be properly instantiated later

    public McpServer(InputStreamReader in, OutputStream out) {
        this.transport = new StdioTransport(in, out);
        this.echoTool = new EchoTool(); // Instantiate the tool
        this.serverCapabilities = initializeServerCapabilities();
    }

    private ServerCapabilities initializeServerCapabilities() {
        ServerCapabilities capabilities = new ServerCapabilities();
        List<ToolDefinition> tools = new ArrayList<>();

        // Define EchoTool
        JsonObject echoInputSchemaProps = new JsonObject();
        echoInputSchemaProps.addProperty("message", "string"); // type: string
        JsonObject echoInputSchema = new JsonObject();
        echoInputSchema.addProperty("type", "object");
        echoInputSchema.add("properties", echoInputSchemaProps);
        // Required fields could be specified here too if needed by schema validator

        JsonObject echoOutputSchemaProps = new JsonObject();
        echoOutputSchemaProps.addProperty("response", "string");
        JsonObject echoOutputSchema = new JsonObject();
        echoOutputSchema.addProperty("type", "object");
        echoOutputSchema.add("properties", echoOutputSchemaProps);


        tools.add(new ToolDefinition(
            this.echoTool.getName(),
            this.echoTool.getDescription(),
            echoInputSchema, // Using JsonObject directly as JsonElement
            echoOutputSchema
        ));
        capabilities.setTools(tools);
        return capabilities;
    }

    public void start() throws IOException {
        LOGGER.info("MCP Server started. Waiting for messages on STDIO...");
        try {
            while (true) {
                JsonRpcMessage message = transport.readMessage();
                if (message == null) {
                    LOGGER.info("Input stream closed (EOF). Shutting down server.");
                    break;
                }

                if (message instanceof JsonRpcRequest) {
                    handleRequest((JsonRpcRequest) message);
                } else if (message instanceof JsonRpcResponse) {
                    LOGGER.warning("Received unexpected JsonRpcResponse: " + JsonRpcParser.toJson(message));
                } else {
                    LOGGER.warning("Received unknown message type: " + message.getClass().getName());
                }
            }
        } catch (IOException e) {
            // Check if this is the "Exit notification received" sentinel
            if ("Exit notification received. Server should shut down.".equals(e.getMessage())) {
                 LOGGER.info("Server shutting down gracefully due to exit notification.");
            } else {
                LOGGER.log(Level.SEVERE, "IOException in server read loop: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in server read loop: " + e.getMessage(), e);
        } finally {
            LOGGER.info("MCP Server main loop ended.");
        }
    }

    private void handleRequest(JsonRpcRequest request) throws IOException {
        LOGGER.info("Received request: method='" + request.getMethod() + "', id='" + request.getId() + "'");
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Request params: " + (request.getParams() != null ? request.getParams().toString() : "null"));
        }

        // Before processing, check if server is initialized for methods that require it.
        // "initialize" is the exception. "shutdown" and "exit" can also be called anytime.
        if (!"initialize".equals(request.getMethod()) && !"shutdown".equals(request.getMethod()) && !"exit".equals(request.getMethod()) && !this.initialized.get()) {
            if (request.getId() != null) {
                LOGGER.warning("Request '" + request.getMethod() + "' received before server is initialized.");
                JsonRpcResponse notInitializedResponse = new JsonRpcResponse(request.getId(),
                        new ErrorObject(JsonRpcErrorCodes.INTERNAL_ERROR, // Or a specific MCP error code for "server not initialized"
                                "Server not initialized. Please send 'initialize' and 'initialized' first.", null));
                transport.writeMessage(notInitializedResponse);
            } else {
                 LOGGER.warning("Notification '" + request.getMethod() + "' received before server is initialized. Ignoring.");
            }
            return;
        }


        JsonRpcResponse response = null;
        try {
            switch (request.getMethod()) {
                case "initialize":
                    if (request.getId() == null) {
                        throw new JsonRpcException(JsonRpcErrorCodes.INVALID_REQUEST, "'initialize' must have an ID.", request.getId());
                    }
                    // InitializeParams params = JsonRpcParser.jsonElementToObject(request.getParams(), InitializeParams.class);
                    // We ignore client capabilities for now.
                    InitializeResult initializeResult = new InitializeResult(this.serverCapabilities);
                    response = new JsonRpcResponse(request.getId(), JsonRpcParser.objectToJsonElement(initializeResult));
                    break;

                case "initialized":
                    if (!request.isNotification()) {
                         LOGGER.warning("'initialized' received with an ID, but it should be a notification. Processing anyway.");
                    }
                    this.initialized.set(true);
                    LOGGER.info("Server initialized by client.");
                    return;

                case "shutdown":
                    if (request.getId() == null) {
                        throw new JsonRpcException(JsonRpcErrorCodes.INVALID_REQUEST, "'shutdown' must have an ID.", request.getId());
                    }
                    LOGGER.info("Shutdown request received. Responding and awaiting exit notification.");
                    this.initialized.set(false);
                    response = new JsonRpcResponse(request.getId(), null);
                    transport.writeMessage(response);
                    return;

                case "exit":
                    LOGGER.info("'exit' notification received. Server will terminate loop.");
                    if (!request.isNotification()) {
                         LOGGER.warning("'exit' received with an ID, but it should be a notification.");
                    }
                    // Throw a specific IOException to signal the main loop to terminate.
                    throw new IOException("Exit notification received. Server should shut down.");

                case "tool/call":
                    response = handleToolCall(request);
                    break;

                default:
                    LOGGER.warning("Method not found: " + request.getMethod());
                    if (request.getId() != null) {
                        response = new JsonRpcResponse(request.getId(),
                            new ErrorObject(JsonRpcErrorCodes.METHOD_NOT_FOUND, "Method not found: " + request.getMethod()));
                    }
                    break;
            }
        } catch (JsonRpcException e) {
            LOGGER.log(Level.SEVERE, "Error processing request '" + request.getMethod() + "': " + e.getMessage(), e);
            Object errorId = e.getRequestId() != null ? e.getRequestId() : request.getId();
            if (errorId != null) {
                 response = new JsonRpcResponse(errorId, new ErrorObject(e.getErrorCode(), e.getMessage(), e.getErrorData()));
            }
        } catch (JsonSyntaxException e) {
            LOGGER.log(Level.SEVERE, "JSON syntax error processing request '" + request.getMethod() + "': " + e.getMessage(), e);
            if (request.getId() != null) {
                response = new JsonRpcResponse(request.getId(),
                    new ErrorObject(JsonRpcErrorCodes.PARSE_ERROR, "Failed to parse parameters: " + e.getMessage()));
            }
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error processing request '" + request.getMethod() + "': " + e.getMessage(), e);
            if (request.getId() != null) {
                response = new JsonRpcResponse(request.getId(),
                    new ErrorObject(JsonRpcErrorCodes.INTERNAL_ERROR, "Internal server error: " + e.getMessage()));
            }
        }

        if (response != null) {
            transport.writeMessage(response);
        } else if (request.getId() != null) {
            LOGGER.warning("No response generated for request with ID: " + request.getId() + " method: " + request.getMethod());
             JsonRpcResponse errorResponse = new JsonRpcResponse(request.getId(),
                    new ErrorObject(JsonRpcErrorCodes.INTERNAL_ERROR, "Internal server error: Failed to generate a response."));
            transport.writeMessage(errorResponse);
        }
    }

    /**
     * Handles a "tool/call" request.
     *
     * @param request The JsonRpcRequest for "tool/call".
     * @return The JsonRpcResponse containing the tool's result or an error.
     * @throws JsonRpcException if parsing parameters fails or tool execution leads to a known error.
     * @throws IOException if writing the response fails.
     */
    private JsonRpcResponse handleToolCall(JsonRpcRequest request) throws JsonRpcException, IOException {
        if (request.getId() == null) {
            // As per JSON-RPC, requests needing a response should have an ID.
            // MCP spec should clarify if tool/call can be a notification. Assuming it cannot.
            LOGGER.warning("tool/call received without an ID. Ignoring as it cannot be responded to.");
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_REQUEST, "tool/call must have an ID.", null);
        }

        ToolCallParams toolCallParams;
        try {
            // Ensure params is a JsonObject before trying to parse it as ToolCallParams
            if (request.getParams() == null || !request.getParams().isJsonObject()) {
                 throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS,
                        "Parameters for tool/call must be a JSON object.", request.getId());
            }
            toolCallParams = JsonRpcParser.jsonElementToObject(request.getParams(), ToolCallParams.class);
            if (toolCallParams == null || toolCallParams.getToolName() == null) {
                 throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS,
                        "Invalid parameters for tool/call: 'toolName' is missing or structure is incorrect.", request.getId());
            }
        } catch (JsonSyntaxException e) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS,
                    "Failed to parse parameters for tool/call: " + e.getMessage(), request.getId(), e);
        }

        LOGGER.info("Handling tool/call for tool: '" + toolCallParams.getToolName() + "' with id: " + request.getId());

        if (echoTool.getName().equals(toolCallParams.getToolName())) {
            // Ensure that the params for the echoTool itself are a JsonObject
            // The EchoTool's call method expects a JsonObject.
            // toolCallParams.getParams() is already a JsonObject from ToolCallParams definition
            JsonObject toolSpecificParams = toolCallParams.getParams();
            if (toolSpecificParams == null) {
                 // This could happen if the "params" field within ToolCallParams was present but null,
                 // or if it was missing and Gson deserialized it to null.
                 // EchoTool's call method does its own null check for its direct params.
                 // However, it's good practice to ensure it's not null if the tool strictly expects it.
                 // For EchoTool, its 'call' method checks for null and missing 'message'.
            }

            JsonObject resultOutput = echoTool.call(toolSpecificParams); // This can throw JsonRpcException
            // The MCP spec for tool/result should be checked.
            // Assuming the direct output of the tool is the "result" for the JSON-RPC response.
            // If tool/result needs a specific wrapper (e.g. { "toolName": "...", "output": ...}),
            // then 'resultOutput' would be wrapped here.
            return new JsonRpcResponse(request.getId(), resultOutput);
        } else {
            LOGGER.warning("Requested tool not found: " + toolCallParams.getToolName());
            throw new JsonRpcException(JsonRpcErrorCodes.METHOD_NOT_FOUND, // Or a more specific MCP tool error code
                    "Tool not found: " + toolCallParams.getToolName(), request.getId());
        }
    }
}
