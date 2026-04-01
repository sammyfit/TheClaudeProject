package com.knorr.teamcenter.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "teamcenter")
public record TeamcenterProperties(
        String serverUrl,
        String username,
        String password
) {
}
