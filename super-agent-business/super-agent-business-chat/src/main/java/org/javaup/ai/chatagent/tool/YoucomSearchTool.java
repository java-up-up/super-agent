package org.javaup.ai.chatagent.tool;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.chatagent.config.YoucomSearchProperties;
import org.javaup.ai.chatagent.model.debug.ChatDebugTrace;
import org.javaup.ai.chatagent.model.debug.ChatToolTrace;
import org.javaup.ai.chatagent.model.SearchReference;
import org.javaup.ai.chatagent.support.ChatContextKeys;
import org.javaup.ai.chatagent.support.RestClientFactorySupport;
import org.javaup.ai.chatagent.support.SinkEmitHelper;
import org.javaup.ai.chatagent.support.StreamEventMetadata;
import org.javaup.ai.chatagent.support.StreamEventWriter;
import org.javaup.ai.chatagent.support.TimeSensitiveQueryHelper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: You.com Search API 工具
 * @author: Claude
 **/
@Slf4j
@Component
public class YoucomSearchTool {

    private final YoucomSearchProperties properties;
    private final StreamEventWriter streamEventWriter;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public YoucomSearchTool(YoucomSearchProperties properties, StreamEventWriter streamEventWriter) {
        this.properties = properties;
        this.streamEventWriter = streamEventWriter;
        this.restClient = RestClientFactorySupport.create(
            properties.getBaseUrl(),
            properties.getConnectTimeoutMs(),
            properties.getReadTimeoutMs()
        );
    }

    public YoucomSearchToolResult search(YoucomSearchRequest request, ToolContext toolContext) {

        String rawQuery = request != null && StrUtil.isNotBlank(request.getQuery()) ? request.getQuery().trim() : "";
        if (StrUtil.isBlank(rawQuery)) {
            throw new IllegalArgumentException("query 不能为空");
        }
        if (!properties.isEnabled()) {
            throw new IllegalStateException("You.com 搜索工具当前已禁用");
        }
        if (StrUtil.isBlank(properties.getApiKey())) {
            throw new IllegalStateException("You.com API Key 未配置");
        }

        long startTime = System.currentTimeMillis();
        ChatToolTrace toolTrace = registerToolTrace(toolContext, ChatToolTrace.builder()
            .toolName("youcom_search")
            .status("RUNNING")
            .inputSummary(rawQuery)
            .build());
        markToolUsed(toolContext, "youcom_search");
        publishThinking(toolContext, "🔍 正在通过 You.com 联网搜索: " + rawQuery);

        try {
            String effectiveQuery = TimeSensitiveQueryHelper.buildEffectiveSearchQuery(
                rawQuery,
                resolveCurrentDate(toolContext)
            );
            if (toolTrace != null) {
                toolTrace.setEffectiveInput(effectiveQuery);
            }

            int maxResults = request != null && request.getMaxResults() != null && request.getMaxResults() > 0
                ? request.getMaxResults()
                : properties.getMaxResults();

            YoucomSearchApiRequest apiRequest = new YoucomSearchApiRequest(
                effectiveQuery,
                maxResults,
                properties.getSafesearch(),
                properties.getFreshness(),
                properties.getCountry(),
                properties.getLanguage()
            );

            String responseBody = restClient.post()
                .uri(properties.getSearchPath())
                .header("X-API-Key", properties.getApiKey())
                .header("Content-Type", "application/json")
                .body(apiRequest)
                .retrieve()
                .body(String.class);

            List<SearchReference> references = parseReferences(responseBody);

            appendReferences(toolContext, references);
            publishThinking(toolContext, "📚 搜索完成，找到 " + references.size() + " 条候选来源");
            completeToolTrace(toolTrace, references.size(), startTime);

            return new YoucomSearchToolResult(effectiveQuery, List.copyOf(references));
        }
        catch (RuntimeException exception) {
            failToolTrace(toolTrace, exception, startTime);
            publishThinking(toolContext, "⚠️ 搜索失败: " + exception.getMessage());
            log.warn("You.com 搜索失败, query={}", rawQuery, exception);
            throw exception;
        }
    }

    private List<SearchReference> parseReferences(String responseBody) {
        List<SearchReference> references = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode webResults = root.path("results").path("web");
            if (webResults.isMissingNode()) {
                webResults = root.path("results");
            }
            if (webResults.isArray()) {
                for (JsonNode item : webResults) {
                    String url = item.path("url").asText("");
                    if (StrUtil.isBlank(url)) {
                        continue;
                    }
                    String title = item.path("title").asText("");
                    String snippet = "";
                    JsonNode snippetsNode = item.path("snippets");
                    if (snippetsNode.isArray() && !snippetsNode.isEmpty()) {
                        snippet = snippetsNode.get(0).asText("");
                    }
                    if (StrUtil.isBlank(snippet)) {
                        snippet = item.path("description").asText("");
                    }
                    SearchReference ref = new SearchReference(title, url, snippet);
                    ref.setToolName("youcom_search");
                    references.add(ref);
                }
            }
        }
        catch (Exception e) {
            log.warn("解析 You.com 搜索响应失败: {}", e.getMessage());
        }
        return references;
    }

    private String resolveCurrentDate(ToolContext toolContext) {
        RunnableConfig config = ToolContextHelper.getConfig(toolContext).orElse(null);
        if (config == null) {
            return "";
        }
        Object value = config.context().get(ChatContextKeys.CURRENT_DATE);
        if (value instanceof String text && StrUtil.isNotBlank(text)) {
            return text.trim();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private void appendReferences(ToolContext toolContext, List<SearchReference> references) {
        if (references.isEmpty()) {
            return;
        }
        RunnableConfig config = ToolContextHelper.getConfig(toolContext).orElse(null);
        if (config == null) {
            return;
        }
        Object container = config.context().get(ChatContextKeys.REFERENCES);
        if (container instanceof List<?> list) {
            ((List<SearchReference>) list).addAll(references);
        }
    }

    @SuppressWarnings("unchecked")
    private void markToolUsed(ToolContext toolContext, String toolName) {
        RunnableConfig config = ToolContextHelper.getConfig(toolContext).orElse(null);
        if (config == null) {
            return;
        }
        Object container = config.context().get(ChatContextKeys.USED_TOOLS);
        if (container instanceof Set<?> set) {
            ((Set<String>) set).add(toolName);
        }
    }

    @SuppressWarnings("unchecked")
    private void publishThinking(ToolContext toolContext, String content) {
        RunnableConfig config = ToolContextHelper.getConfig(toolContext).orElse(null);
        if (config == null) {
            return;
        }
        Object sinkCandidate = config.context().get(ChatContextKeys.EVENT_SINK);
        StreamEventMetadata metadata = resolveMetadata(config);
        if (sinkCandidate instanceof Sinks.Many<?> sink) {
            SinkEmitHelper.emitNext((Sinks.Many<String>) sink, streamEventWriter.thinking(content, metadata));
        }
        Object stepsCandidate = config.context().get(ChatContextKeys.THINKING_STEPS);
        if (stepsCandidate instanceof List<?> list) {
            ((List<String>) list).add(content);
        }
    }

    private StreamEventMetadata resolveMetadata(RunnableConfig config) {
        if (config == null) {
            return null;
        }
        Object metadataCandidate = config.context().get(ChatContextKeys.EVENT_METADATA);
        if (metadataCandidate instanceof StreamEventMetadata metadata) {
            return metadata;
        }
        return null;
    }

    private ChatToolTrace registerToolTrace(ToolContext toolContext, ChatToolTrace trace) {
        if (trace == null) {
            return null;
        }
        RunnableConfig config = ToolContextHelper.getConfig(toolContext).orElse(null);
        if (config == null) {
            return trace;
        }
        Object candidate = config.context().get(ChatContextKeys.DEBUG_TRACE);
        if (candidate instanceof ChatDebugTrace debugTrace) {
            debugTrace.getToolTraces().add(trace);
        }
        return trace;
    }

    private void completeToolTrace(ChatToolTrace toolTrace, int referenceCount, long startTime) {
        if (toolTrace == null) {
            return;
        }
        toolTrace.setStatus("COMPLETED");
        toolTrace.setReferenceCount(referenceCount);
        toolTrace.setDurationMs(Math.max(0L, System.currentTimeMillis() - startTime));
        toolTrace.setOutputSummary("You.com 联网结果已返回，候选来源 " + referenceCount + " 条");
    }

    private void failToolTrace(ChatToolTrace toolTrace, RuntimeException exception, long startTime) {
        if (toolTrace == null) {
            return;
        }
        toolTrace.setStatus("FAILED");
        toolTrace.setDurationMs(Math.max(0L, System.currentTimeMillis() - startTime));
        toolTrace.setErrorMessage(exception == null ? "" : StrUtil.blankToDefault(exception.getMessage(), ""));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class YoucomSearchApiRequest {
        private String query;
        private Integer count;
        @JsonProperty("safesearch")
        private String safesearch;
        private String freshness;
        private String country;
        private String language;
    }
}
