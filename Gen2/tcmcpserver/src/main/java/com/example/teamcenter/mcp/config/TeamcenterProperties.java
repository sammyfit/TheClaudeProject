package com.example.teamcenter.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration properties for Teamcenter connection.
 * Bound from application.properties under the "teamcenter." prefix.
 */
@ConfigurationProperties(prefix = "teamcenter")
public record TeamcenterProperties(
        String serverUrl,
        String username,
        String password
) {
}
