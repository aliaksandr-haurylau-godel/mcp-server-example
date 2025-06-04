package io.modelcontextprotocol.server;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.After;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder;
import java.lang.Process;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

// Imports for JSON handling
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class McpServerFunctionalTest {

    private Process serverProcess;
    private OutputStream serverStdin;
    private InputStream serverStdout;
    private InputStream serverStderr; // For reading error output from server

    private Gson gson = new Gson();
    private int nextId = 1;

    @Before
    public void setUp() throws IOException, InterruptedException { // Added InterruptedException
        this.nextId = 1; // Reset ID for each test
        ProcessBuilder pb = new ProcessBuilder("mvn", "compile", "exec:java", "-Dexec.cleanupDaemonThreads=false");
        serverProcess = pb.start();
        serverStdin = serverProcess.getOutputStream();
        serverStdout = serverProcess.getInputStream();
        serverStderr = serverProcess.getErrorStream();
        Thread.sleep(3000); // Increased sleep to 3s
    }

    @After
    public void tearDown() throws IOException, InterruptedException {
        if (serverProcess != null && serverProcess.isAlive()) {
            // Attempt graceful shutdown by closing stdin first
            // (MCP messages for shutdown/exit will be added in a later step)
            if (serverStdin != null) {
                try {
                    serverStdin.close();
                } catch (IOException e) {
                    // Log or handle, but don't let it stop further cleanup
                    System.err.println("Error closing server STDIN: " + e.getMessage());
                }
            }

            boolean exited = serverProcess.waitFor(5, TimeUnit.SECONDS);
            if (!exited) {
                System.err.println("Server process did not exit gracefully, forcing termination.");
                serverProcess.destroyForcibly();
                if (!serverProcess.waitFor(5, TimeUnit.SECONDS)) {
                    System.err.println("Server process did not terminate after force destroy.");
                }
            }
        }

        // Ensure streams are closed
        if (serverStdin != null) try { serverStdin.close(); } catch (IOException e) { /* ignore */ }
        if (serverStdout != null) try { serverStdout.close(); } catch (IOException e) { /* ignore */ }
        if (serverStderr != null) try { serverStderr.close(); } catch (IOException e) { /* ignore */ }
    }

    // Actual test methods will be added in subsequent steps.
    // For example:
    // @Test
    // public void testInitialize() throws IOException {
    //     // Logic to send initialize request and assert response
    // }

    @Test
    public void testInitializeSequence() throws IOException {
        JsonObject initializeParams = new JsonObject();
        // Optional: Add client capabilities
        // JsonObject clientCapabilities = new JsonObject();
        // initializeParams.add("capabilities", clientCapabilities);
        sendMessage("initialize", initializeParams);

        JsonObject initializeResult = receiveMessage();
        assertNotNull("Initialize response should not be null", initializeResult);
        assertTrue("Initialize response should have a 'result' field", initializeResult.has("result"));
        assertEquals("ID of initialize response should match request", this.nextId - 1, initializeResult.get("id").getAsInt());
        assertTrue("Initialize result should have 'capabilities'", initializeResult.getAsJsonObject("result").has("capabilities"));
        // Example: Assert specific capability if known and static
        // assertTrue(initializeResult.getAsJsonObject("result").getAsJsonObject("capabilities").has("someSpecificCapability"));

        sendMessage("initialized", new JsonObject()); // Or null for params
        // No response expected for 'initialized' notification
    }

    @Test
    public void testEchoToolCall() throws IOException {
        // 1. Full Initialize sequence
        JsonObject initializeParams = new JsonObject();
        sendMessage("initialize", initializeParams);

        JsonObject initializeResult = receiveMessage();
        assertNotNull("Initialize response should not be null (echo test)", initializeResult);
        if (initializeResult.has("error") && !initializeResult.get("error").isJsonNull()) {
            fail("Server returned error during initialize (echo test): " + initializeResult.get("error").toString());
        }
        assertTrue("Initialize response should have a 'result' field (echo test)", initializeResult.has("result"));
        assertEquals("ID of initialize response should match request (echo test)", this.nextId - 1, initializeResult.get("id").getAsInt());
        assertTrue("Initialize result should have 'capabilities' (echo test)", initializeResult.getAsJsonObject("result").has("capabilities"));

        sendMessage("initialized", new JsonObject()); // Or null for params

        // 2. Actual tool call
        JsonObject toolParams = new JsonObject();
        toolParams.addProperty("message", "Hello Server!");

        JsonObject callParams = new JsonObject();
        callParams.addProperty("toolName", "echoTool");
        callParams.add("params", toolParams);
        sendMessage("tool/call", callParams);

        JsonObject toolResult = receiveMessage();
        assertNotNull("Tool call response should not be null", toolResult);

        if (toolResult.has("error") && !toolResult.get("error").isJsonNull()) {
            // Read STDERR for more clues if server sent an error
            String serverErrorOutput = readStream(serverStderr, 1000); // Read for up to 1 sec
            if (serverErrorOutput != null && !serverErrorOutput.isEmpty()) {
                System.err.println("Server STDERR output on tool/call error:\n" + serverErrorOutput);
            }
            fail("Server returned error for tool/call: " + toolResult.get("error").toString());
        }

        assertTrue("Tool call response should have a 'result' field", toolResult.has("result"));
        assertEquals("ID of tool call response should match request", this.nextId - 1, toolResult.get("id").getAsInt());

        // Check if result is JsonNull before trying to get it as JsonObject
        if (toolResult.get("result").isJsonNull()) {
            fail("Tool call result is null, expected a JsonObject with echo response.");
        }
        JsonObject resultPayload = toolResult.getAsJsonObject("result");

        assertTrue("Tool result payload should have 'response'", resultPayload.has("response"));
        assertEquals("EchoTool response mismatch", "Echo: Hello Server!", resultPayload.get("response").getAsString());
    }

    @Test
    public void testShutdownSequence() throws IOException {
        // Optional: perform a quick initialize first if server requires it before shutdown.
        // For this test, we'll assume shutdown can be called on a fresh server,
        // or that other tests might have initialized it (though ideally tests are independent).
        // To make it more robust, a minimal init can be done:
        JsonObject initializeParamsForShutdown = new JsonObject();
        sendMessage("initialize", initializeParamsForShutdown);
        JsonObject initializeResultForShutdown = receiveMessage(); // Consume response
        assertNotNull("Initialize response for shutdown test should not be null", initializeResultForShutdown);
        if (initializeResultForShutdown.has("error") && !initializeResultForShutdown.get("error").isJsonNull()) {
            // Don't fail the test here, main point is to test shutdown, but log it.
            System.err.println("Server returned error during initialize (shutdown test), proceeding with shutdown: " + initializeResultForShutdown.get("error").toString());
        } else {
            // Only send initialized if initialize didn't error
             sendMessage("initialized", new JsonObject());
        }

        sendMessage("shutdown", null); // Or new JsonObject()

        JsonObject shutdownResult = receiveMessage();
        assertNotNull("Shutdown response should not be null", shutdownResult);

        if (shutdownResult.has("error") && !shutdownResult.get("error").isJsonNull()) {
            fail("Server returned error for shutdown: " + shutdownResult.get("error").toString());
        }

        // Server should respond with a result field, typically null for shutdown
        assertTrue("Shutdown response should have a 'result' field (even if null)", shutdownResult.has("result"));
        if (shutdownResult.has("result") && !shutdownResult.get("result").isJsonNull()) {
             // If result is not null, it might be an issue, but primary check is for error.
             // For now, let's be strict: spec implies result should be null.
            assertTrue("Shutdown result should be JsonNull", shutdownResult.get("result").isJsonNull());
        } else if (!shutdownResult.has("result")) {
             fail("Shutdown response is missing 'result' field.");
        }
        // If we are here, result is present and null, or it was missing and we failed, or it had an error and we failed.
        // So, if it's present and null, we can proceed to check ID.
        if(shutdownResult.has("result") && shutdownResult.get("result").isJsonNull()){
             assertEquals("ID of shutdown response should match request", this.nextId - 1, shutdownResult.get("id").getAsInt());
        }


        sendMessage("exit", null); // Or new JsonObject()
        // No response expected for 'exit' notification.
        // tearDown will verify process termination.
        // We might need to give the server a moment to process exit before tearDown closes streams.
        try {
            TimeUnit.MILLISECONDS.sleep(500); // Give server time to process exit
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendMessage(String method, JsonObject params) throws IOException {
        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", "2.0");
        request.addProperty("method", method);
        if (params != null) {
            request.add("params", params);
        }
        // ID is added for requests expecting a response, not for notifications like 'exit' or 'initialized'
        if (!("exit".equals(method) || "initialized".equals(method))) {
            request.addProperty("id", this.nextId++);
        }

        String jsonRequest = gson.toJson(request);
        byte[] messageBytes = jsonRequest.getBytes(StandardCharsets.UTF_8);
        String header = "Content-Length: " + messageBytes.length + "\r\n\r\n";

        System.out.println("Client -> Server: " + header.trim() + jsonRequest); // For debugging

        serverStdin.write(header.getBytes(StandardCharsets.UTF_8));
        serverStdin.write(messageBytes);
        serverStdin.flush();
    }

    private JsonObject receiveMessage() throws IOException {
        BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(serverStdout, StandardCharsets.UTF_8));
        StringBuilder headers = new StringBuilder();
        String line;
        int contentLength = -1;

        // Read header lines until an empty line is encountered
        while ((line = stdoutReader.readLine()) != null && !line.isEmpty()) {
            System.out.println("Server -> Client (Header): " + line); // For debugging
            headers.append(line).append("\r\n");
            if (line.toLowerCase().startsWith("content-length:")) {
                try {
                    contentLength = Integer.parseInt(line.substring("content-length:".length()).trim());
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid Content-Length value: " + line, e);
                }
            }
        }

        if (contentLength == -1) {
            String errOutput = readStream(serverStderr, 500); // Read with a short timeout
            if (errOutput != null && !errOutput.isEmpty()) {
                 System.err.println("Server STDERR before failing to read Content-Length:\n" + errOutput);
            }
            throw new IOException("Content-Length header not found or empty line not found. Server output headers: " + headers.toString());
        }

        // Read JSON body
        char[] bodyChars = new char[contentLength];
        int charsRead = 0;
        while (charsRead < contentLength) {
            int result = stdoutReader.read(bodyChars, charsRead, contentLength - charsRead);
            if (result == -1) {
                String errOutput = readStream(serverStderr, 500); // Read with a short timeout
                if (errOutput != null && !errOutput.isEmpty()) {
                     System.err.println("Server STDERR during body read:\n" + errOutput);
                }
                throw new IOException("End of stream reached while reading JSON body. Expected " + contentLength + " chars, got " + charsRead);
            }
            charsRead += result;
        }
        String jsonBody = new String(bodyChars);

        System.out.println("Server -> Client (Body): " + jsonBody); // For debugging
        try {
            return JsonParser.parseString(jsonBody).getAsJsonObject();
        } catch (com.google.gson.JsonSyntaxException e) {
            throw new IOException("Failed to parse JSON response: " + jsonBody, e);
        }
    }

    private String readStream(InputStream stream, int timeoutMillis) throws IOException {
        if (stream == null) return ""; // Stream might not be initialized (e.g. if process failed to start)
        if (stream.available() == 0 && timeoutMillis == 0) return ""; // Nothing to read immediately and no timeout

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        long startTime = System.currentTimeMillis();
        byte[] buffer = new byte[1024];

        // Non-blocking read if timeoutMillis is 0
        if (timeoutMillis == 0) {
            if(stream.available() > 0) {
                int length = stream.read(buffer, 0, Math.min(buffer.length, stream.available()));
                if (length != -1) {
                    result.write(buffer, 0, length);
                }
            }
            return result.toString(StandardCharsets.UTF_8.name());
        }

        // Blocking read with timeout
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            if (stream.available() > 0) {
                int length = stream.read(buffer, 0, Math.min(buffer.length, stream.available()));
                if (length == -1) break;
                result.write(buffer, 0, length);
            } else {
                // Avoid busy-waiting if nothing is available
                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }
}
