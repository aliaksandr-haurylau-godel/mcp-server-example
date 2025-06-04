package io.modelcontextprotocol.server;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;


/**
 * Main entry point for the MCP Server.
 */
public class Main {
    private static final Logger ROOT_LOGGER = Logger.getLogger(""); // Get root logger
    private static final Logger APP_LOGGER = Logger.getLogger(Main.class.getPackage().getName()); // Get package logger

    /**
     * Configures basic logging for the application.
     * By default, logs INFO and above to the console.
     * If a "mcp-server.log" file can be written, it logs FINE and above to it.
     */
    private static void setupLogging() {
        // Remove default handlers to prevent duplicate console output if any exist
        ROOT_LOGGER.setUseParentHandlers(false); // For root logger, this might not be needed if configuring directly
        for (Handler handler : ROOT_LOGGER.getHandlers()) {
            ROOT_LOGGER.removeHandler(handler);
        }

        // Configure Console Handler
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO); // Console logs INFO and above
        consoleHandler.setFormatter(new SimpleFormatter());
        ROOT_LOGGER.addHandler(consoleHandler); // Add to root logger

        try {
            // Configure File Handler
            FileHandler fileHandler = new FileHandler("mcp-server.log", true); // Append mode
            fileHandler.setLevel(Level.FINE); // File logs FINE and above (more detailed)
            fileHandler.setFormatter(new SimpleFormatter()); // Or XMLFormatter for more structure
            ROOT_LOGGER.addHandler(fileHandler);
            APP_LOGGER.info("File logging initialized to mcp-server.log");
        } catch (IOException e) {
            APP_LOGGER.log(Level.WARNING, "Could not initialize file logging to mcp-server.log: " + e.getMessage(), e);
        }

        // Set level for application specific loggers if needed, e.g., to be more verbose than root
        APP_LOGGER.setLevel(Level.FINE); // Or whatever default you want for your app's own loggers

        // Example: Set specific logger levels if needed
        // Logger.getLogger("io.modelcontextprotocol.server.transport.StdioTransport").setLevel(Level.FINER);
    }


    public static void main(String[] args) {
        setupLogging(); // Initialize basic logging configuration

        APP_LOGGER.info("Starting MCP Server...");

        try {
            // It's crucial that System.in is wrapped in an InputStreamReader
            // that correctly handles the input stream's encoding.
            // The MCP spec implies UTF-8 for JSON content.
            InputStreamReader stdinReader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
            OutputStream stdoutStream = System.out;

            McpServer server = new McpServer(stdinReader, stdoutStream);
            APP_LOGGER.info("McpServer instance created. Starting listening loop...");
            server.start(); // This method will block until the server shuts down

            APP_LOGGER.info("MCP Server has shut down normally.");

        } catch (UnsupportedEncodingException e) {
            // This should ideally not happen with StandardCharsets.UTF_8
            APP_LOGGER.log(Level.SEVERE, "UTF-8 encoding not supported, cannot start server.", e);
            System.err.println("Fatal Error: UTF-8 encoding not supported. Cannot start server.");
        } catch (IOException e) {
            // This might catch issues if server.start() itself throws an IOException
            // not handled internally (e.g., catastrophic failure in transport setup not caught by start's try-catch).
            APP_LOGGER.log(Level.SEVERE, "MCP Server encountered a critical I/O error and had to terminate.", e);
        } catch (Exception e) {
            APP_LOGGER.log(Level.SEVERE, "MCP Server encountered an unexpected critical error and had to terminate.", e);
        } finally {
            APP_LOGGER.info("MCP Server main method finished.");
            // Ensure all log messages are written
            for (Handler handler : ROOT_LOGGER.getHandlers()) {
                handler.flush();
                handler.close();
            }
        }
    }
}
