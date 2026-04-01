package com.knorr.teamcenter.mcp.config;

import com.knorr.teamcenter.mcp.tc.TeamcenterSessionManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
