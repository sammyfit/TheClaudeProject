package com.knorr.teamcenter.mcp.tools;

import com.knorr.teamcenter.mcp.tc.TeamcenterSessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TeamcenterSearchTools {

    private static final Logger log = LoggerFactory.getLogger(TeamcenterSearchTools.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final TeamcenterSessionManager sessionManager;

    public TeamcenterSearchTools(TeamcenterSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Tool(description = "Search for Items in Teamcenter PLM by keyword. "
            + "Returns item ID, name, type, owner, modification date, and release status. "
            + "Use this for general item lookups when you need to find parts, assemblies, "
            + "or documents by name.")
    public String tc_search_items(
            @ToolParam(description = "Search keyword to find items by name. "
                    + "Wildcards are added automatically around the keyword.")
            String keyword,

            @ToolParam(description = "Maximum number of results to return. Default is 25.",
                    required = false)
            Integer maxResults
    ) {
        int limit = (maxResults != null && maxResults > 0) ? maxResults : 25;
        log.info("tc_search_items called: keyword='{}', maxResults={}", keyword, limit);

        return sessionManager.executeWithRetry(() -> {
            // --- STUB for development/testing ---
            List<Map<String, String>> stubResults = new ArrayList<>();
            Map<String, String> item1 = new LinkedHashMap<>();
            item1.put("uid", "xrt5ABC123def456");
            item1.put("item_id", "000123");
            item1.put("object_name", "Sample " + keyword + " Assembly");
            item1.put("object_type", "Item");
            item1.put("owning_user", sessionManager.getUsername());
            item1.put("last_mod_date", "2025-03-15T10:30:00Z");
            item1.put("release_status", "Released");
            stubResults.add(item1);

            Map<String, String> item2 = new LinkedHashMap<>();
            item2.put("uid", "xrt5DEF789ghi012");
            item2.put("item_id", "000456");
            item2.put("object_name", keyword + " Sub-Component Rev B");
            item2.put("object_type", "ItemRevision");
            item2.put("owning_user", sessionManager.getUsername());
            item2.put("last_mod_date", "2025-04-01T14:15:00Z");
            item2.put("release_status", "In Work");
            stubResults.add(item2);

            log.info("Found {} results for keyword '{}'", stubResults.size(), keyword);
            return toJson(Map.of("results", stubResults, "count", stubResults.size()));
        });
    }

    @Tool(description = "List all available Saved Queries in Teamcenter. "
            + "Use this to discover what types of searches are available before "
            + "calling tc_execute_saved_query. Returns query names that can be "
            + "passed to tc_execute_saved_query.")
    public String tc_list_saved_queries() {
        log.info("tc_list_saved_queries called");

        return sessionManager.executeWithRetry(() -> {
            List<Map<String, String>> queries = List.of(
                    Map.of("name", "__WEB_find_Items", "description", "Search Items by ID or Name"),
                    Map.of("name", "__WEB_find_Item_Revision", "description", "Search Item Revisions"),
                    Map.of("name", "Item Name", "description", "Find items by name pattern"),
                    Map.of("name", "Item...", "description", "General item search"),
                    Map.of("name", "__WEB_find_Changes", "description", "Search Change objects (ECN/ECR)")
            );
            return toJson(Map.of("saved_queries", queries, "count", queries.size()));
        });
    }

    @Tool(description = "Execute a specific Teamcenter Saved Query with criteria. "
            + "First use tc_list_saved_queries to discover available queries, then call "
            + "this with the exact query name and field criteria. "
            + "Example: queryName='Item Name', criteriaJson='{\"Name\": \"*bracket*\"}'")
    public String tc_execute_saved_query(
            @ToolParam(description = "The exact name of the Teamcenter saved query to execute. "
                    + "Get available names from tc_list_saved_queries.")
            String queryName,

            @ToolParam(description = "JSON object of key-value pairs where keys are query field "
                    + "names and values are search values. Supports wildcards with *. "
                    + "Example: {\"Item ID\": \"000*\", \"Name\": \"*widget*\"}")
            String criteriaJson,

            @ToolParam(description = "Maximum number of results to return. Default is 25.",
                    required = false)
            Integer maxResults
    ) {
        int limit = (maxResults != null && maxResults > 0) ? maxResults : 25;
        log.info("tc_execute_saved_query called: query='{}', criteria={}", queryName, criteriaJson);

        // Validate criteria JSON upfront
        Map<String, String> criteria;
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> parsed = mapper.readValue(criteriaJson, Map.class);
            criteria = parsed;
        } catch (Exception e) {
            log.error("Invalid criteria JSON: {}", criteriaJson, e);
            throw new IllegalArgumentException(
                    "Invalid criteria JSON. Expected format: {\"Field\": \"Value\"}. Error: " + e.getMessage());
        }

        final Map<String, String> finalCriteria = criteria;
        return sessionManager.executeWithRetry(() -> {
            String[] entries = finalCriteria.keySet().toArray(new String[0]);
            String[] values = finalCriteria.values().toArray(new String[0]);

            // --- STUB ---
            List<Map<String, String>> stubResults = new ArrayList<>();
            Map<String, String> item = new LinkedHashMap<>();
            item.put("uid", "xrt5XYZ789abc012");
            item.put("item_id", "000789");
            item.put("object_name", "Query Result for " + queryName);
            item.put("object_type", "Item");
            item.put("owning_user", sessionManager.getUsername());
            item.put("last_mod_date", "2025-02-20T09:00:00Z");
            item.put("release_status", "Released");
            item.put("matched_criteria", criteriaJson);
            stubResults.add(item);

            log.info("Executed '{}' with {} criteria, found {} results",
                    queryName, entries.length, stubResults.size());
            return toJson(Map.of(
                    "query", queryName,
                    "results", stubResults,
                    "count", stubResults.size()
            ));
        });
    }

    private String toJson(Object obj) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize result to JSON", e);
            return "{\"error\": \"Failed to serialize result\"}";
        }
    }
}
