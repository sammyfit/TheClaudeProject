package com.knorr.teamcenter.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Teamcenter MCP Server Application.
 *
 * A Spring Boot application that exposes Siemens Teamcenter PLM operations
 * as MCP tools over Streamable HTTP transport. Designed for integration with
 * Microsoft Copilot Studio (or any MCP-compatible client).
 *
 * Architecture:
 *   Copilot (MCP Client) --Streamable HTTP--> This App --SOA/HTTP--> Teamcenter
 *
 * The Spring AI MCP Server Boot Starter auto-configures:
 *   - Streamable HTTP transport at /mcp
 *   - Tool discovery and registration from @McpTool annotated beans
 *   - JSON-RPC message handling
 */
@SpringBootApplication
public class TeamcenterMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeamcenterMcpServerApplication.class, args);
    }
}
