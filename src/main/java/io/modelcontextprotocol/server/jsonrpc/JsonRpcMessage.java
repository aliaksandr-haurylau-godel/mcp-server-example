package io.modelcontextprotocol.server.jsonrpc;

/**
 * Base class for JSON-RPC messages.
 * Ensures the "jsonrpc" field is present.
 */
public abstract class JsonRpcMessage {
    private final String jsonrpc = "2.0";

    public String getJsonrpc() {
        return jsonrpc;
    }
}
