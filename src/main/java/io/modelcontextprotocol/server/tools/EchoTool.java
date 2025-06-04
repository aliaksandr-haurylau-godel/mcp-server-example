package io.modelcontextprotocol.server.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.modelcontextprotocol.server.jsonrpc.JsonRpcException; // For parameter validation
import io.modelcontextprotocol.server.jsonrpc.JsonRpcErrorCodes; // For error codes

/**
 * A simple tool that echoes back a given message.
 */
public class EchoTool {

    private static final String NAME = "echoTool";
    private static final String DESCRIPTION = "A simple tool that takes a 'message' string and returns it prefixed with 'Echo: '.";
    private static final String INPUT_PARAM_MESSAGE = "message";
    private static final String OUTPUT_PARAM_RESPONSE = "response";


    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Executes the echo tool logic.
     *
     * @param params A JsonObject expected to contain a "message" field with a string value.
     * @return A JsonObject containing a "response" field with the echoed message.
     * @throws JsonRpcException if the parameters are invalid (e.g., missing "message" or not a string).
     */
    public JsonObject call(JsonObject params) throws JsonRpcException {
        if (params == null || !params.has(INPUT_PARAM_MESSAGE)) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS,
                    "Parameter '" + INPUT_PARAM_MESSAGE + "' is missing for " + NAME, null); // ID is null as tool call doesn't know it
        }

        if (!params.get(INPUT_PARAM_MESSAGE).isJsonPrimitive() || !params.getAsJsonPrimitive(INPUT_PARAM_MESSAGE).isString()) {
            throw new JsonRpcException(JsonRpcErrorCodes.INVALID_PARAMS,
                    "Parameter '" + INPUT_PARAM_MESSAGE + "' must be a string for " + NAME, null);
        }

        String originalMessage = params.get(INPUT_PARAM_MESSAGE).getAsString();
        String echoedMessage = "Echo: " + originalMessage;

        JsonObject result = new JsonObject();
        result.add(OUTPUT_PARAM_RESPONSE, new JsonPrimitive(echoedMessage));

        return result;
    }
}
