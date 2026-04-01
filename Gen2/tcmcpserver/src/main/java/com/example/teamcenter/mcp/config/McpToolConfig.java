package com.example.teamcenter.mcp.config;

import com.example.teamcenter.mcp.tools.TeamcenterSearchTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers Teamcenter MCP tools with the Spring AI MCP Server.
 *
 * The ToolCallbackProvider bean is auto-detected by the MCP Server Boot Starter,
 * which registers each @Tool method as an MCP tool available to clients.
 */
@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider teamcenterTools(TeamcenterSearchTools searchTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(searchTools)
                .build();
    }
}
