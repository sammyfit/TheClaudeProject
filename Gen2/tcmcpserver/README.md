# Teamcenter MCP Server

A **Spring Boot** application that exposes Siemens Teamcenter PLM operations as
MCP (Model Context Protocol) tools over **Streamable HTTP** transport, designed
for integration with **Microsoft Copilot Studio** and other MCP-compatible clients.

## Architecture

```
┌─────────────────────┐      ┌──────────────────────────────┐      ┌──────────────┐
│                     │      │  Spring Boot App              │      │              │
│  Microsoft Copilot  │──────│                               │──────│  Teamcenter  │
│  Studio             │ HTTP │  /mcp (Streamable HTTP)       │ SOA  │  Server      │
│  (MCP Client)       │──────│  Spring AI MCP Server Starter │──────│  (4-tier)    │
│                     │      │  @McpTool annotated services  │      │              │
└─────────────────────┘      └──────────────────────────────┘      └──────────────┘
```

**Key design decisions:**
- **Streamable HTTP** (not SSE) — Copilot Studio deprecated SSE transport after August 2025
  and requires Streamable HTTP for MCP server connections.
- **Spring AI MCP Server Boot Starter** — provides auto-configuration, annotation-based tool
  registration, and protocol handling out of the box.
- **Synchronous server type** — Teamcenter SOA calls are blocking, so SYNC mode is appropriate.
- **Lazy session initialization** — TC login happens on first tool invocation, not at app startup.

## Prerequisites

- **Java 17+**
- **Maven 3.8+**
- **Teamcenter SOA Client JARs** from your TC installation's `soa_client.zip`

## Project Structure

```
src/main/java/com/example/teamcenter/mcp/
├── TeamcenterMcpServerApplication.java   # Spring Boot entry point
├── config/
│   ├── TeamcenterProperties.java         # Type-safe TC config properties
│   ├── TeamcenterConfig.java             # TC session manager bean
│   ├── McpToolConfig.java                # MCP tool registration
│   └── TeamcenterHealthIndicator.java    # Actuator health check
├── tc/
│   └── TeamcenterSessionManager.java     # TC SOA session lifecycle
└── tools/
    └── TeamcenterSearchTools.java        # @McpTool annotated search tools
```

## Setup: Installing TC SOA JARs

The Teamcenter SOA client JARs are proprietary and not in Maven Central:

```bash
# Extract from your Teamcenter distribution media
unzip soa_client.zip

# Install each JAR into your local Maven repository
mvn install:install-file \
  -Dfile=java/libs/TcSoaClient.jar \
  -DgroupId=com.siemens.teamcenter \
  -DartifactId=tcsoaclient \
  -Dversion=14.0 -Dpackaging=jar

mvn install:install-file \
  -Dfile=java/libs/TcSoaCommon.jar \
  -DgroupId=com.siemens.teamcenter \
  -DartifactId=tcsoacommon \
  -Dversion=14.0 -Dpackaging=jar

mvn install:install-file \
  -Dfile=java/libs/TcSoaStrongModel_Query.jar \
  -DgroupId=com.siemens.teamcenter \
  -DartifactId=tcsoaquery_strong \
  -Dversion=14.0 -Dpackaging=jar

mvn install:install-file \
  -Dfile=java/libs/TcSoaStrongModel_Core.jar \
  -DgroupId=com.siemens.teamcenter \
  -DartifactId=tcsoacore_strong \
  -Dversion=14.0 -Dpackaging=jar
```

Then **uncomment** the TC dependencies in `pom.xml`.

## Build & Run

```bash
# Build
mvn clean package

# Run (with TC credentials)
TC_SERVER_URL=https://your-tc-server:7001/tc \
TC_USERNAME=your_user \
TC_PASSWORD=your_pass \
java -jar target/teamcenter-mcp-server-1.0.0-SNAPSHOT.jar
```

The MCP endpoint will be available at: `http://localhost:8080/mcp`

## Test with MCP Inspector

Before connecting Copilot, verify with the MCP Inspector:

```bash
npx @modelcontextprotocol/inspector
```

Set **Transport Type** to "Streamable HTTP" and **URL** to `http://localhost:8080/mcp`.
Click Connect, then list and invoke tools.

## Microsoft Copilot Studio Integration

### Connecting via MCP Onboarding Wizard

1. Open your agent in **Copilot Studio**
2. Navigate to **Settings > AI Capabilities** (or **Tools > Add a Tool**)
3. Select **New tool > MCP**
4. Configure:
   - **Transport**: Streamable HTTP
   - **Server URL**: `https://your-deployed-host/mcp`
   - **Authentication**: Configure as needed (API Key or OAuth 2.0)
5. Click **Test Connection** — you should see the three tools listed
6. Add the tools to your agent

### Important Notes for Copilot Studio

- The server **must be reachable over HTTPS** in production
- Deploy behind a reverse proxy with TLS (e.g., Azure App Service, NGINX, etc.)
- Copilot Studio connects using Streamable HTTP transport (SSE is deprecated)
- Consider adding authentication (see "Adding Security" section below)

## Available MCP Tools

| Tool                       | Description                                        |
|----------------------------|----------------------------------------------------|
| `tc_search_items`          | Keyword search for Items by name                   |
| `tc_list_saved_queries`    | List all available Teamcenter saved queries         |
| `tc_execute_saved_query`   | Run a specific saved query with field criteria      |

## Configuration Reference

| Property                          | Description                          | Default                      |
|-----------------------------------|--------------------------------------|------------------------------|
| `teamcenter.server-url`           | TC 4-tier server URL                 | `https://localhost:7001/tc`  |
| `teamcenter.username`             | TC login user                        | `infodba`                    |
| `teamcenter.password`             | TC login password                    | `infodba`                    |
| `server.port`                     | HTTP port for MCP server             | `8080`                       |
| `spring.ai.mcp.server.protocol`   | Transport protocol                   | `STREAMABLE`                 |

Environment variables `TC_SERVER_URL`, `TC_USERNAME`, `TC_PASSWORD` override the defaults.

## Adding More Tools

To add new tools (BOM expansion, workflow, change management):

1. Create a new `@Service` class in `tools/` with `@McpTool` annotated methods
2. Register it in `McpToolConfig` by adding it to the `ToolCallbackProvider`
3. Rebuild and redeploy — Copilot Studio auto-discovers new tools

Example:

```java
@Service
public class TeamcenterBomTools {

    @Tool(description = "Expand the Bill of Materials (BOM) for a given Item Revision UID")
    public String tc_expand_bom(
            @ToolParam(description = "UID of the ItemRevision to expand") String revisionUid,
            @ToolParam(description = "Number of levels to expand (0 = all)", required = false) Integer levels
    ) {
        // SOA call to StructureManagementService.expandPSOneLevel() or expandPSAllLevels()
    }
}
```

## Adding Security

For production deployment with Copilot Studio, add OAuth 2.0 or API Key auth:

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>mcp-server-security</artifactId>
    <version>0.1.3</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

See: https://github.com/spring-ai-community/mcp-security

## Health Check

The app exposes an actuator endpoint:

```bash
curl http://localhost:8080/actuator/health
```

## Deployment Options

- **Azure App Service** — Recommended for Copilot Studio integration
- **Azure Container Apps** — For containerized deployments
- **Docker** — Build with `mvn spring-boot:build-image`
- **On-premises** — Deploy near your Teamcenter server for lowest latency

## Troubleshooting

- **ClassNotFoundError for TC classes**: soa_client JARs not on classpath
- **Session timeout**: The server automatically re-authenticates on expiry
- **Copilot can't connect**: Ensure HTTPS, check firewall rules, verify /mcp endpoint
- **Tools not showing in Copilot**: Check Test Connection, review server logs for errors
