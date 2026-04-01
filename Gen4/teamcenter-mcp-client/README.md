# Teamcenter MCP Test Client

A standalone interactive CLI client for testing your Teamcenter MCP Server.
Connects via **Streamable HTTP** — the same transport that Microsoft Copilot Studio uses.

## Architecture

```
┌──────────────────────────┐         ┌──────────────────────────┐
│  This Client (CLI)       │  HTTP   │  Teamcenter MCP Server   │
│                          │────────▶│  (Spring Boot)           │
│  McpClient               │  /mcp   │                          │
│  StreamableHttpTransport │◀────────│  Streamable HTTP         │
└──────────────────────────┘         └──────────────────────────┘
```

## Build

```bash
mvn clean package
```

## Run

First, make sure your Teamcenter MCP Server is running (default: `http://localhost:8080`).

```bash
# Default (connects to http://localhost:8080)
java -jar target/teamcenter-mcp-client-1.0.0-SNAPSHOT.jar

# Custom server URL
java -jar target/teamcenter-mcp-client-1.0.0-SNAPSHOT.jar --mcp.server.url=http://your-server:8080

# Or via environment variable
MCP_SERVER_URL=http://your-server:8080 java -jar target/teamcenter-mcp-client-1.0.0-SNAPSHOT.jar
```

## Interactive Commands

```
Commands:
  1 - Search items (tc_search_items)
  2 - List saved queries (tc_list_saved_queries)
  3 - Execute saved query (tc_execute_saved_query)
  4 - Ping server
  5 - List tools (with schemas)
  6 - Call any tool by name
  q - Quit
```

## Example Session

```
╔══════════════════════════════════════════════════════════════╗
║           Teamcenter MCP Test Client                        ║
╚══════════════════════════════════════════════════════════════╝

Connecting to MCP server at: http://localhost:8080

[OK] Connected and MCP handshake completed successfully!
[OK] Server ping successful.

Available tools (3):
────────────────────────────────────────────────────────────────
  [1] tc_search_items
      Search for Items in Teamcenter PLM by keyword...

  [2] tc_list_saved_queries
      List all available Saved Queries in Teamcenter...

  [3] tc_execute_saved_query
      Execute a specific Teamcenter Saved Query with criteria...

────────────────────────────────────────────────────────────────
Enter command: 1
Enter search keyword: bracket
Max results [25]: 10

Calling tc_search_items(keyword="bracket", maxResults=10)...

[RESULT]:
{
  "results" : [ {
    "uid" : "xrt5ABC123def456",
    "item_id" : "000123",
    "object_name" : "Sample bracket Assembly",
    ...
  } ],
  "count" : 2
}
```
