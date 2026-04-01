package com.knorr.teamcenter.mcp.config;

import com.knorr.teamcenter.mcp.tools.TeamcenterSearchTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider teamcenterTools(TeamcenterSearchTools searchTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(searchTools)
                .build();
    }
}
