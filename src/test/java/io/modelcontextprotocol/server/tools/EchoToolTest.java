package io.modelcontextprotocol.server.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.modelcontextprotocol.server.jsonrpc.JsonRpcException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class EchoToolTest {

    private EchoTool echoTool;

    @Before
    public void setUp() {
        echoTool = new EchoTool();
    }

    @Test
    public void testGetName() {
        assertEquals("echoTool", echoTool.getName());
    }

    @Test
    public void testGetDescription() {
        assertEquals("A simple tool that takes a 'message' string and returns it prefixed with 'Echo: '.", echoTool.getDescription());
    }

    @Test
    public void testCallSuccessful() throws JsonRpcException {
        JsonObject params = new JsonObject();
        params.addProperty("message", "Hello");

        JsonObject result = echoTool.call(params);

        assertNotNull(result);
        assertEquals("Echo: Hello", result.get("response").getAsString());
    }

    @Test
    public void testCallMissingMessageParameter() {
        JsonObject params = new JsonObject();
        // "message" parameter is missing
        try {
            echoTool.call(params);
            fail("Expected JsonRpcException for missing parameter");
        } catch (JsonRpcException e) {
            assertEquals(-32602, e.getErrorCode()); // Invalid Params
            assertEquals("Parameter 'message' is missing for echoTool", e.getMessage());
        }
    }

    @Test
    public void testCallNullParameters() {
        try {
            echoTool.call(null);
            fail("Expected JsonRpcException for null parameters");
        } catch (JsonRpcException e) {
            assertEquals(-32602, e.getErrorCode()); // Invalid Params
            assertEquals("Parameter 'message' is missing for echoTool", e.getMessage());
        }
    }

    @Test
    public void testCallMessageParameterNotString() {
        JsonObject params = new JsonObject();
        params.add("message", new JsonPrimitive(123)); // Not a string

        try {
            echoTool.call(params);
            fail("Expected JsonRpcException for non-string message parameter");
        } catch (JsonRpcException e) {
            assertEquals(-32602, e.getErrorCode()); // Invalid Params
            assertEquals("Parameter 'message' must be a string for echoTool", e.getMessage());
        }
    }
}
