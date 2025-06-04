package io.modelcontextprotocol.server.jsonrpc;

/**
 * Standard JSON-RPC 2.0 Error codes.
 * See https://www.jsonrpc.org/specification#error_object
 */
public final class JsonRpcErrorCodes {
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    // -32000 to -32099: Server error (implementation-defined)

    private JsonRpcErrorCodes() { /* Private constructor to prevent instantiation */ }
}
