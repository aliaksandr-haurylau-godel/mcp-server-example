package io.modelcontextprotocol.server.transport;

import io.modelcontextprotocol.server.jsonrpc.JsonRpcMessage;
import io.modelcontextprotocol.server.jsonrpc.JsonRpcParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles message transport over STDIO with Content-Length headers.
 */
public class StdioTransport {

    private static final Logger LOGGER = Logger.getLogger(StdioTransport.class.getName());
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";
    // MCP spec does not explicitly require Content-Type for STDIO, but it's good practice for general JSON-RPC.
    // For strict adherence, we might omit it if not specified for STDIO transport.
    // private static final String CONTENT_TYPE_HEADER = "Content-Type";
    // private static final String DEFAULT_CHARSET = StandardCharsets.UTF_8.name();
    private static final String HEADER_SEPARATOR = "\r\n";

    private final BufferedReader reader;
    private final OutputStream outputStream;

    public StdioTransport(InputStreamReader inputStreamReader, OutputStream outputStream) {
        this.reader = new BufferedReader(inputStreamReader);
        this.outputStream = outputStream;
    }

    /**
     * Reads a single JSON-RPC message from the input stream.
     * This method blocks until a complete message is received or EOF.
     *
     * @return The parsed JsonRpcMessage, or null if EOF is reached cleanly before a message starts.
     * @throws IOException if an I/O error occurs or message framing is invalid.
     */
    public JsonRpcMessage readMessage() throws IOException {
        Map<String, String> headers = readHeaders();

        if (headers.isEmpty()) {
            // This indicates EOF was reached before any headers could be read.
            LOGGER.info("Clean EOF detected before any headers.");
            return null;
        }

        String contentLengthStr = headers.get(CONTENT_LENGTH_HEADER);
        if (contentLengthStr == null) {
            LOGGER.severe("Missing Content-Length header. Headers received: " + headers);
            // This is a protocol violation if headers were received but Content-Length is missing.
            throw new IOException("Missing Content-Length header after receiving other headers.");
        }

        int contentLength;
        try {
            contentLength = Integer.parseInt(contentLengthStr.trim());
        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid Content-Length value: '" + contentLengthStr + "'", e);
            throw new IOException("Invalid Content-Length value: '" + contentLengthStr + "'", e);
        }

        if (contentLength < 0) {
            LOGGER.severe("Negative Content-Length: " + contentLength);
            throw new IOException("Negative Content-Length: " + contentLength);
        }

        // Read the message body
        char[] buffer = new char[contentLength];
        int totalBytesRead = 0;
        while(totalBytesRead < contentLength) {
            int bytesRead = reader.read(buffer, totalBytesRead, contentLength - totalBytesRead);
            if (bytesRead == -1) { // EOF
                LOGGER.severe("EOF reached unexpectedly while reading message body. Expected " + contentLength + " bytes, got " + totalBytesRead);
                throw new IOException("EOF reached unexpectedly while reading message body. Expected " + contentLength + ", read " + totalBytesRead);
            }
            totalBytesRead += bytesRead;
        }
        String jsonContent = new String(buffer, 0, totalBytesRead);

        LOGGER.fine("Received JSON (" + totalBytesRead + " chars): " + jsonContent);

        try {
            return JsonRpcParser.parse(jsonContent);
        } catch (Exception e) { // Catching generic Exception from parser for now (e.g. JsonSyntaxException)
            LOGGER.log(Level.SEVERE, "Failed to parse JSON message: " + jsonContent, e);
            // According to JSON-RPC 2.0, if a parse error occurs, the server SHOULD respond with an error.
            // However, at the transport layer, we might just signal a bad message.
            // For now, rethrow as IOException to indicate a transport/content issue.
            // The main server loop can decide to send a JSON-RPC error if appropriate.
            throw new IOException("Failed to parse JSON message: " + e.getMessage(), e);
        }
    }

    /**
     * Reads headers from the input stream.
     * Headers are read until a blank line (\r\n) is encountered.
     * Header names are treated case-insensitively (keys in map are stored as read).
     *
     * @return A map of header names to values. Returns empty map if EOF is reached before any headers.
     * @throws IOException if an I/O error occurs (not for clean EOF before headers).
     *                     Throws IOException if EOF occurs *after* some headers but *before* the separating blank line.
     */
    private Map<String, String> readHeaders() throws IOException {
        Map<String, String> headers = new HashMap<>();
        String line;
        while (true) {
            line = reader.readLine(); // Can throw IOException
            if (line == null) {
                // EOF reached.
                if (!headers.isEmpty()) {
                    // EOF occurred after some headers were read but before the terminating empty line.
                    LOGGER.warning("EOF reached after some headers but before the separating CRLF for message body.");
                    throw new IOException("Stream ended unexpectedly after message headers and before body separator.");
                }
                // Clean EOF before any headers were read.
                LOGGER.info("EOF reached while attempting to read header line.");
                return headers; // Return empty map
            }

            if (line.isEmpty()) { // An empty line signifies the end of headers
                LOGGER.finer("End of headers detected.");
                break;
            }

            LOGGER.finer("Read header line: " + line);
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String name = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                // Store header name as read, or normalize (e.g. to lower-case or Title-Case for consistent access)
                // For Content-Length, LSP uses it as-is.
                headers.put(name, value);
            } else {
                LOGGER.warning("Malformed header line (no colon or colon at start): '" + line + "'. Skipping.");
                // Strict parsing might throw an exception here.
            }
        }
        LOGGER.fine("Read headers: " + headers);
        return headers;
    }

    /**
     * Writes a JSON-RPC message to the output stream.
     * Prepends Content-Length header and \r\n separator.
     *
     * @param message The JsonRpcMessage to send.
     * @throws IOException if an I/O error occurs.
     */
    public void writeMessage(JsonRpcMessage message) throws IOException {
        if (message == null) {
            LOGGER.warning("Attempted to write a null message. Skipping.");
            return;
        }
        String jsonContent = JsonRpcParser.toJson(message);
        byte[] jsonBytes = jsonContent.getBytes(StandardCharsets.UTF_8);

        // Construct headers. MCP spec for STDIO transport only mentions Content-Length.
        // "Content-Length: %d\r\n\r\n%s" where %d is length and %s is content.
        String headerString =
            CONTENT_LENGTH_HEADER + ": " + jsonBytes.length + HEADER_SEPARATOR +
            HEADER_SEPARATOR; // Final separator between headers and body

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Sending message with headers: [" + headerString.replace(HEADER_SEPARATOR, "\\r\\n") + "]");
            LOGGER.fine("Sending JSON ("+ jsonBytes.length +" bytes): " + jsonContent);
        }


        // The outputStream.write methods can throw IOException
        outputStream.write(headerString.getBytes(StandardCharsets.US_ASCII)); // Headers are typically ASCII
        outputStream.write(jsonBytes);
        outputStream.flush(); // Ensure the message is sent immediately
        LOGGER.finer("Message flushed to output stream.");
    }
}
