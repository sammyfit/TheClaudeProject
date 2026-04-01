package com.knorr.teamcenter.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Teamcenter MCP Server.
 *
 * These tests:
 *   1. Start the full Spring Boot app on a random port
 *   2. Create an MCP client using Streamable HTTP transport
 *   3. Connect to the running server and invoke tools
 *   4. Verify the responses
 *
 * This is exactly what Copilot Studio (or any MCP client) will do in production,
 * so these tests validate end-to-end MCP protocol compliance.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TeamcenterMcpServerIntegrationTest {

    @LocalServerPort
    private int port;

    private McpSyncClient mcpClient;

    @BeforeEach
    void setUp() {
        // Create an MCP client that connects to our running server via Streamable HTTP.
        // This is the same transport Microsoft Copilot uses.
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder("http://localhost:" + port)
                .endpoint("/mcp")
                .build();

        mcpClient = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .build();

        // Initialize performs the MCP handshake (protocol version negotiation, capability exchange)
        mcpClient.initialize();
    }

    @AfterEach
    void tearDown() {
        if (mcpClient != null) {
            mcpClient.close();
        }
    }

    // =========================================================================
    // Test 1: Verify MCP connection and handshake
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("Should successfully connect and complete MCP handshake")
    void shouldConnectAndInitialize() {
        // If we got here without exception, the MCP handshake succeeded.
        // Let's also verify the server responds to ping.
        assertDoesNotThrow(() -> mcpClient.ping());
    }

    // =========================================================================
    // Test 2: List available tools
    // =========================================================================

    @Test
    @Order(2)
    @DisplayName("Should list all registered Teamcenter tools")
    void shouldListTools() {
        ListToolsResult toolsResult = mcpClient.listTools();

        assertNotNull(toolsResult);
        assertNotNull(toolsResult.tools());
        assertFalse(toolsResult.tools().isEmpty(), "Server should expose at least one tool");

        // Verify our three tools are registered
        var toolNames = toolsResult.tools().stream()
                .map(McpSchema.Tool::name)
                .toList();

        assertTrue(toolNames.contains("tc_search_items"),
                "Should have tc_search_items tool. Found: " + toolNames);
        assertTrue(toolNames.contains("tc_list_saved_queries"),
                "Should have tc_list_saved_queries tool. Found: " + toolNames);
        assertTrue(toolNames.contains("tc_execute_saved_query"),
                "Should have tc_execute_saved_query tool. Found: " + toolNames);

        // Verify tools have descriptions (important for LLM to know when to call them)
        for (McpSchema.Tool tool : toolsResult.tools()) {
            assertNotNull(tool.description(),
                    "Tool " + tool.name() + " should have a description");
            assertFalse(tool.description().isBlank(),
                    "Tool " + tool.name() + " description should not be blank");
        }
    }

    // =========================================================================
    // Test 3: Invoke tc_search_items
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("Should search items by keyword")
    void shouldSearchItemsByKeyword() {
        CallToolResult result = mcpClient.callTool(
                new CallToolRequest("tc_search_items", Map.of(
                        "keyword", "bracket",
                        "maxResults", 10
                ))
        );

        assertNotNull(result);
        assertFalse(result.isError(), "Tool call should not return an error");
        assertNotNull(result.content());
        assertFalse(result.content().isEmpty(), "Should return content");

        // The result content should be a TextContent with JSON
        String responseText = extractText(result);
        assertNotNull(responseText);

        // Verify the JSON response contains expected fields
        assertTrue(responseText.contains("item_id"), "Response should contain item_id");
        assertTrue(responseText.contains("object_name"), "Response should contain object_name");
        assertTrue(responseText.contains("bracket"), "Response should contain the search keyword");
    }

    @Test
    @Order(4)
    @DisplayName("Should search items with default maxResults when not provided")
    void shouldSearchWithDefaultMaxResults() {
        CallToolResult result = mcpClient.callTool(
                new CallToolRequest("tc_search_items", Map.of(
                        "keyword", "widget"
                ))
        );

        assertNotNull(result);
        assertFalse(result.isError());

        String responseText = extractText(result);
        assertTrue(responseText.contains("widget"),
                "Response should contain the keyword 'widget'");
    }

    // =========================================================================
    // Test 4: Invoke tc_list_saved_queries
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("Should list available saved queries")
    void shouldListSavedQueries() {
        CallToolResult result = mcpClient.callTool(
                new CallToolRequest("tc_list_saved_queries", Map.of())
        );

        assertNotNull(result);
        assertFalse(result.isError(), "Tool call should not return an error");

        String responseText = extractText(result);
        assertNotNull(responseText);

        // Verify known saved queries appear in the response
        assertTrue(responseText.contains("saved_queries"),
                "Response should contain saved_queries key");
        assertTrue(responseText.contains("__WEB_find_Items"),
                "Response should include __WEB_find_Items query");
        assertTrue(responseText.contains("count"),
                "Response should include count");
    }

    // =========================================================================
    // Test 5: Invoke tc_execute_saved_query
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("Should execute a saved query with criteria")
    void shouldExecuteSavedQuery() {
        CallToolResult result = mcpClient.callTool(
                new CallToolRequest("tc_execute_saved_query", Map.of(
                        "queryName", "Item Name",
                        "criteriaJson", "{\"Name\": \"*bracket*\"}",
                        "maxResults", 5
                ))
        );

        assertNotNull(result);
        assertFalse(result.isError(), "Tool call should not return an error");

        String responseText = extractText(result);
        assertNotNull(responseText);

        // Verify the response structure
        assertTrue(responseText.contains("query"),
                "Response should contain the query name");
        assertTrue(responseText.contains("results"),
                "Response should contain results array");
        assertTrue(responseText.contains("Item Name"),
                "Response should echo the query name");
    }

    @Test
    @Order(7)
    @DisplayName("Should execute a saved query with multiple criteria")
    void shouldExecuteSavedQueryWithMultipleCriteria() {
        CallToolResult result = mcpClient.callTool(
                new CallToolRequest("tc_execute_saved_query", Map.of(
                        "queryName", "__WEB_find_Items",
                        "criteriaJson", "{\"Item ID\": \"000*\", \"Name\": \"*assembly*\"}",
                        "maxResults", 25
                ))
        );

        assertNotNull(result);
        assertFalse(result.isError());

        String responseText = extractText(result);
        assertTrue(responseText.contains("__WEB_find_Items"));
    }

    // =========================================================================
    // Test 6: Error handling
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("Should handle invalid JSON criteria gracefully")
    void shouldHandleInvalidCriteriaJson() {
        CallToolResult result = mcpClient.callTool(
                new CallToolRequest("tc_execute_saved_query", Map.of(
                        "queryName", "Item Name",
                        "criteriaJson", "not-valid-json",
                        "maxResults", 5
                ))
        );

        assertNotNull(result);
        // The tool should return an error result (isError=true) rather than crashing
        assertTrue(result.isError(), "Should return error for invalid JSON criteria");

        String responseText = extractText(result);
        assertNotNull(responseText);
    }

    // =========================================================================
    // Test 7: Tool schema validation
    // =========================================================================

    @Test
    @Order(9)
    @DisplayName("Should have proper input schemas on all tools")
    void shouldHaveProperInputSchemas() {
        ListToolsResult toolsResult = mcpClient.listTools();

        for (McpSchema.Tool tool : toolsResult.tools()) {
            assertNotNull(tool.inputSchema(),
                    "Tool " + tool.name() + " should have an input schema");

            // Log tool details for debugging
            System.out.println("Tool: " + tool.name());
            System.out.println("  Description: " + tool.description());
            System.out.println("  Schema: " + tool.inputSchema());
            System.out.println();
        }
    }

    // =========================================================================
    // Utility
    // =========================================================================

    /**
     * Extracts text content from a CallToolResult.
     * MCP tool results contain a list of content blocks, typically TextContent.
     */
    private String extractText(CallToolResult result) {
        if (result.content() == null || result.content().isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (McpSchema.Content content : result.content()) {
            if (content instanceof McpSchema.TextContent textContent) {
                sb.append(textContent.text());
            }
        }
        return sb.toString();
    }
}
