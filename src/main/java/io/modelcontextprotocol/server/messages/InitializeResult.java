package io.modelcontextprotocol.server.messages;

/**
 * Result of the 'initialize' request. Contains server capabilities.
 */
public class InitializeResult {
    private ServerCapabilities capabilities;
    // private ServerInfo serverInfo; // Optional server info (name, version) as per LSP

    public InitializeResult(ServerCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    public ServerCapabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(ServerCapabilities capabilities) {
        this.capabilities = capabilities;
    }
}
