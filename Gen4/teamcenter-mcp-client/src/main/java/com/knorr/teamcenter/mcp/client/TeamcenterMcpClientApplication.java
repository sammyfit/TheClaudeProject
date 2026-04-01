package com.knorr.teamcenter.mcp.client;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Standalone MCP Test Client for the Teamcenter MCP Server.
 *
 * Connects to your MCP server via Streamable HTTP and provides an
 * interactive command-line interface to:
 *   - List available tools
 *   - Call tools with parameters
 *   - Ping the server
 *   - View tool schemas
 *
 * Usage:
 *   java -jar teamcenter-mcp-client.jar
 *   java -jar teamcenter-mcp-client.jar --mcp.server.url=http://your-host:8080
 */
@SpringBootApplication
public class TeamcenterMcpClientApplication implements CommandLineRunner {

    @Value("${mcp.server.url}")
    private String serverUrl;

    public static void main(String[] args) {
        SpringApplication.run(TeamcenterMcpClientApplication.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           Teamcenter MCP Test Client                        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // --- 1. Connect to MCP Server ---
        System.out.println("Connecting to MCP server at: " + serverUrl);
        System.out.println();

        McpSyncClient client = null;
        try {
            HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                    .builder(serverUrl)
                    .endpoint("/mcp")
                    .build();

            client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(30))
                    .build();

            client.initialize();
            System.out.println("[OK] Connected and MCP handshake completed successfully!");
            System.out.println();

            // --- 2. Ping ---
            client.ping();
            System.out.println("[OK] Server ping successful.");
            System.out.println();

            // --- 3. List tools ---
            ListToolsResult toolsResult = client.listTools();
            System.out.println("Available tools (" + toolsResult.tools().size() + "):");
            System.out.println("─".repeat(60));
            for (int i = 0; i < toolsResult.tools().size(); i++) {
                McpSchema.Tool tool = toolsResult.tools().get(i);
                System.out.printf("  [%d] %s%n", i + 1, tool.name());
                System.out.printf("      %s%n", tool.description());
                System.out.println();
            }

            // --- 4. Interactive loop ---
            interactiveLoop(client, toolsResult);

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to connect: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (client != null) {
                try {
                    client.close();
                    System.out.println("Disconnected from MCP server.");
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    private void interactiveLoop(McpSyncClient client, ListToolsResult toolsResult) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("─".repeat(60));
            System.out.println("Commands:");
            System.out.println("  1 - Search items (tc_search_items)");
            System.out.println("  2 - List saved queries (tc_list_saved_queries)");
            System.out.println("  3 - Execute saved query (tc_execute_saved_query)");
            System.out.println("  4 - Ping server");
            System.out.println("  5 - List tools (with schemas)");
            System.out.println("  6 - Call any tool by name");
            System.out.println("  q - Quit");
            System.out.println();
            System.out.print("Enter command: ");

            String input = scanner.nextLine().trim();

            switch (input) {
                case "1" -> callSearchItems(client, scanner);
                case "2" -> callListSavedQueries(client);
                case "3" -> callExecuteSavedQuery(client, scanner);
                case "4" -> {
                    client.ping();
                    System.out.println("[OK] Ping successful.");
                }
                case "5" -> listToolsWithSchemas(client);
                case "6" -> callCustomTool(client, scanner);
                case "q", "Q", "quit", "exit" -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Unknown command: " + input);
            }
            System.out.println();
        }
    }

    // =========================================================================
    // Command handlers
    // =========================================================================

    private void callSearchItems(McpSyncClient client, Scanner scanner) {
        System.out.print("Enter search keyword: ");
        String keyword = scanner.nextLine().trim();

        System.out.print("Max results [25]: ");
        String maxStr = scanner.nextLine().trim();
        int maxResults = maxStr.isEmpty() ? 25 : Integer.parseInt(maxStr);

        System.out.println();
        System.out.println("Calling tc_search_items(keyword=\"" + keyword + "\", maxResults=" + maxResults + ")...");
        System.out.println();

        try {
            CallToolResult result = client.callTool(
                    new CallToolRequest("tc_search_items", Map.of(
                            "keyword", keyword,
                            "maxResults", maxResults
                    ))
            );
            printResult(result);
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    private void callListSavedQueries(McpSyncClient client) {
        System.out.println();
        System.out.println("Calling tc_list_saved_queries()...");
        System.out.println();

        try {
            CallToolResult result = client.callTool(
                    new CallToolRequest("tc_list_saved_queries", Map.of())
            );
            printResult(result);
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    private void callExecuteSavedQuery(McpSyncClient client, Scanner scanner) {
        System.out.print("Enter saved query name: ");
        String queryName = scanner.nextLine().trim();

        System.out.println("Enter criteria as JSON (e.g., {\"Name\": \"*bracket*\"}):");
        System.out.print("> ");
        String criteriaJson = scanner.nextLine().trim();

        System.out.print("Max results [25]: ");
        String maxStr = scanner.nextLine().trim();
        int maxResults = maxStr.isEmpty() ? 25 : Integer.parseInt(maxStr);

        System.out.println();
        System.out.println("Calling tc_execute_saved_query(queryName=\"" + queryName + "\")...");
        System.out.println();

        try {
            CallToolResult result = client.callTool(
                    new CallToolRequest("tc_execute_saved_query", Map.of(
                            "queryName", queryName,
                            "criteriaJson", criteriaJson,
                            "maxResults", maxResults
                    ))
            );
            printResult(result);
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    private void listToolsWithSchemas(McpSyncClient client) {
        ListToolsResult tools = client.listTools();
        System.out.println();
        System.out.println("Tools with schemas:");
        System.out.println("═".repeat(60));

        for (McpSchema.Tool tool : tools.tools()) {
            System.out.println("Tool: " + tool.name());
            System.out.println("Description: " + tool.description());
            System.out.println("Input Schema: " + tool.inputSchema());
            System.out.println("─".repeat(60));
        }
    }

    private void callCustomTool(McpSyncClient client, Scanner scanner) {
        System.out.print("Enter tool name: ");
        String toolName = scanner.nextLine().trim();

        Map<String, Object> params = new LinkedHashMap<>();
        System.out.println("Enter parameters (key=value format, empty line to finish):");
        while (true) {
            System.out.print("  param> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) break;

            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();

                // Try to parse as number
                try {
                    params.put(key, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    params.put(key, value);
                }
            } else {
                System.out.println("  Invalid format. Use key=value");
            }
        }

        System.out.println();
        System.out.println("Calling " + toolName + " with params: " + params);
        System.out.println();

        try {
            CallToolResult result = client.callTool(new CallToolRequest(toolName, params));
            printResult(result);
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    // =========================================================================
    // Output helpers
    // =========================================================================

    private void printResult(CallToolResult result) {
        if (result.isError()) {
            System.out.println("[ERROR] Tool returned an error:");
        } else {
            System.out.println("[RESULT]:");
        }
        System.out.println();

        for (McpSchema.Content content : result.content()) {
            if (content instanceof McpSchema.TextContent textContent) {
                System.out.println(textContent.text());
            } else {
                System.out.println("(non-text content: " + content.getClass().getSimpleName() + ")");
            }
        }
    }
}
