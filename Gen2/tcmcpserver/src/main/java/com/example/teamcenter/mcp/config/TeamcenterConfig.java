package com.example.teamcenter.mcp.config;

import com.example.teamcenter.mcp.tc.TeamcenterSessionManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Teamcenter-related beans.
 */
@Configuration
@EnableConfigurationProperties(TeamcenterProperties.class)
public class TeamcenterConfig {

    @Bean
    public TeamcenterSessionManager teamcenterSessionManager(TeamcenterProperties props) {
        return new TeamcenterSessionManager(
                props.serverUrl(),
                props.username(),
                props.password()
        );
    }
}
