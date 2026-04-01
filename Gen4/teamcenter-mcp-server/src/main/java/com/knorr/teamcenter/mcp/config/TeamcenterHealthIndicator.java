package com.knorr.teamcenter.mcp.config;

import com.knorr.teamcenter.mcp.tc.TeamcenterSessionManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class TeamcenterHealthIndicator implements HealthIndicator {

    private final TeamcenterSessionManager sessionManager;

    public TeamcenterHealthIndicator(TeamcenterSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Health health() {
        if (sessionManager.isLoggedIn()) {
            return Health.up()
                    .withDetail("teamcenter", "Connected")
                    .withDetail("user", sessionManager.getUsername())
                    .build();
        } else {
            return Health.up()
                    .withDetail("teamcenter", "Not yet connected (lazy init)")
                    .build();
        }
    }
}
