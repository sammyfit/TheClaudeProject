package com.knorr.teamcenter.mcp.tc;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeamcenterSessionManager {

    private static final Logger log = LoggerFactory.getLogger(TeamcenterSessionManager.class);

    private final String tcServerUrl;
    private final String username;
    private final String password;

    private boolean loggedIn = false;

    public TeamcenterSessionManager(String tcServerUrl, String username, String password) {
        this.tcServerUrl = tcServerUrl;
        this.username = username;
        this.password = password;
    }

    public synchronized void ensureSession() {
        if (loggedIn) {
            return;
        }
        login();
    }

    /**
     * Real implementation:
     * <pre>
     *   AppCredentialManager credManager = new AppCredentialManager(username, password);
     *   connection = new Connection(tcServerUrl, credManager, "REST", "HTTP");
     *   SessionService sessionService = SessionService.getService(connection);
     *   LoginResponse loginResp = sessionService.login(username, password, "", "", "", null);
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

    @PreDestroy
    public synchronized void logout() {
        if (!loggedIn) return;
        log.info("Logging out from Teamcenter");
        // TODO: SessionService.getService(connection).logout();
        loggedIn = false;
    }

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
