package io.modelcontextprotocol.server.jsonrpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Helper class for parsing JSON-RPC messages using Gson.
 * This class will also handle serializing them.
 */
public class JsonRpcParser {

    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    /**
     * Parses a JSON string into a JsonRpcRequest or JsonRpcResponse.
     * This is a basic version; it determines type based on presence of "method" or "result"/"error".
     *
     * @param jsonString The JSON string to parse.
     * @return A JsonRpcMessage (either JsonRpcRequest or JsonRpcResponse).
     * @throws JsonSyntaxException if the JSON is invalid or doesn't fit expected structures.
     */
    public static JsonRpcMessage parse(String jsonString) throws JsonSyntaxException {
        JsonObject jsonObj = gson.fromJson(jsonString, JsonObject.class);

        if (jsonObj == null) {
            throw new JsonSyntaxException("Input JSON string is null or empty.");
        }
        if (!jsonObj.has("jsonrpc") || !jsonObj.get("jsonrpc").getAsString().equals("2.0")) {
            // This check is more for protocol conformance than pure parsing
            // For now, we assume it's there as per our object model.
        }

        if (jsonObj.has("method")) {
            return gson.fromJson(jsonString, JsonRpcRequest.class);
        } else if (jsonObj.has("result") || jsonObj.has("error")) {
            return gson.fromJson(jsonString, JsonRpcResponse.class);
        } else {
            // Could be an invalid message or a notification response (which isn't standard)
            // For now, we'll throw an error if it's not clearly a request or response.
            // A more robust parser might try to deserialize to a generic JsonObject
            // and then decide, or have more specific checks.
            throw new JsonSyntaxException("JSON string is not a valid JSON-RPC Request or Response: " + jsonString);
        }
    }

    /**
     * Attempts to parse a JSON string specifically as a JsonRpcRequest.
     * @param jsonString the JSON string
     * @return JsonRpcRequest object
     * @throws JsonSyntaxException if parsing fails
     */
    public static JsonRpcRequest parseRequest(String jsonString) throws JsonSyntaxException {
        return gson.fromJson(jsonString, JsonRpcRequest.class);
    }

    /**
     * Serializes a JsonRpcMessage (Request, Response) object to its JSON string representation.
     *
     * @param message The JsonRpcMessage object.
     * @return The JSON string representation.
     */
    public static String toJson(JsonRpcMessage message) {
        return gson.toJson(message);
    }

    /**
     * Helper to convert an arbitrary object to JsonElement.
     * @param object the object to convert
     * @return JsonElement representation
     */
    public static JsonElement objectToJsonElement(Object object) {
        if (object == null) {
            return null;
        }
        return gson.toJsonTree(object);
    }

    /**
     * Helper to convert JsonElement to a specific class.
     * @param jsonElement the json element
     * @param clazz the target class
     * @param <T> the type of the target class
     * @return an instance of the target class
     * @throws JsonSyntaxException if conversion fails
     */
    public static <T> T jsonElementToObject(JsonElement jsonElement, Class<T> clazz) throws JsonSyntaxException {
        if (jsonElement == null) {
            return null;
        }
        return gson.fromJson(jsonElement, clazz);
    }
}
