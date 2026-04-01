package com.example.teamcenter.mcp.tc;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the Teamcenter SOA session lifecycle.
 *
 * Responsibilities:
 *  - Lazy login on first tool invocation (not at app startup)
 *  - Session caching across multiple MCP tool calls
 *  - Automatic re-authentication on session expiry
 *  - Clean logout on application shutdown
 *
 * IMPORTANT: Uncomment the real TC SOA imports and calls once you have
 * the soa_client JARs on your classpath. The exact patterns are shown
 * in Javadoc comments throughout.
 */
public class TeamcenterSessionManager {

    private static final Logger log = LoggerFactory.getLogger(TeamcenterSessionManager.class);

    private final String tcServerUrl;
    private final String username;
    private final String password;

    // === Replace with real TC SOA types when JARs are available ===
    // private Connection connection;
    private boolean loggedIn = false;

    public TeamcenterSessionManager(String tcServerUrl, String username, String password) {
        this.tcServerUrl = tcServerUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * Ensures we have a live Teamcenter session. Called before every SOA operation.
     */
    public synchronized void ensureSession() {
        if (loggedIn) {
            return;
        }
        login();
    }

    /**
     * Establishes a Teamcenter SOA connection and authenticates.
     *
     * Real implementation:
     * <pre>
     *   // 1. Create CredentialManager
     *   AppCredentialManager credManager = new AppCredentialManager(username, password);
     *
     *   // 2. Create a Connection (4-tier HTTP)
     *   connection = new Connection(tcServerUrl, credManager, "REST", "HTTP");
     *
     *   // 3. Get SessionService and login
     *   SessionService sessionService = SessionService.getService(connection);
     *   LoginResponse loginResp = sessionService.login(username, password, "", "", "", null);
     *
     *   // 4. Check for errors
     *   if (loginResp.serviceData.sizeOfPartialErrors() > 0) {
     *       throw new RuntimeException("TC login failed: "
     *           + loginResp.serviceData.getPartialError(0).getMessages()[0]);
     *   }
     * </pre>
     */
    private void login() {
        log.info("Connecting to Teamcenter at {} as user '{}'", tcServerUrl, username);
        // TODO: Replace with real SOA login calls above
        log.info("[STUB] Teamcenter login simulated successfully");
        loggedIn = true;
    }

    /**
     * Logs out from Teamcenter. Called on application shutdown via @PreDestroy.
     */
    @PreDestroy
    public synchronized void logout() {
        if (!loggedIn) return;
        log.info("Logging out from Teamcenter");
        // TODO: SessionService.getService(connection).logout();
        loggedIn = false;
    }

    /**
     * Execute a TC operation with automatic session recovery.
     * If the operation fails with a session-expired error, re-authenticates and retries once.
     */
    public <T> T executeWithRetry(TcOperation<T> operation) {
        ensureSession();
        try {
            return operation.execute();
        } catch (Exception e) {
            if (isSessionExpiredError(e)) {
                log.warn("Teamcenter session expired, re-authenticating...");
                loggedIn = false;
                ensureSession();
                try {
                    return operation.execute();
                } catch (Exception retryEx) {
                    throw new RuntimeException("TC operation failed after re-login", retryEx);
                }
            }
            throw new RuntimeException("TC operation failed", e);
        }
    }

    private boolean isSessionExpiredError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("session") && (msg.contains("expired") || msg.contains("invalid"))
                || msg.contains("515024");
    }

    /**
     * Returns the underlying TC connection for direct SOA service access.
     * Call ensureSession() before using this.
     *
     * Real implementation:
     * <pre>
     *   public Connection getConnection() {
     *       ensureSession();
     *       return connection;
     *   }
     * </pre>
     */
    // public Connection getConnection() { ... }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public String getUsername() {
        return username;
    }

    @FunctionalInterface
    public interface TcOperation<T> {
        T execute() throws Exception;
    }
}
