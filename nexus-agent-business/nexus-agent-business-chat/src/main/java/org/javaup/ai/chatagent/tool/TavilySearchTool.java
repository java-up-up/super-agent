package org.javaup.ai.chatagent.tool;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.chatagent.config.TavilySearchProperties;
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
import java.util.Locale;
import java.util.Set;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 工具类
 * @author: 阿星不是程序员
 **/
@Slf4j
@Component
public class TavilySearchTool {

    private static final Set<String> ALLOWED_TOPICS = Set.of("general", "news", "finance");

    private final TavilySearchProperties properties;
    private final StreamEventWriter streamEventWriter;
    private final RestClient restClient;

    public TavilySearchTool(TavilySearchProperties properties, StreamEventWriter streamEventWriter) {
        this.properties = properties;
        this.streamEventWriter = streamEventWriter;
        this.restClient = RestClientFactorySupport.create(
            properties.getBaseUrl(),
            properties.getConnectTimeoutMs(),
            properties.getReadTimeoutMs()
        );
    }

    public TavilySearchToolResult search(TavilySearchRequest request, ToolContext toolContext) {

        String rawQuery = request != null && StrUtil.isNotBlank(request.getQuery()) ? request.getQuery().trim() : "";
        if (StrUtil.isBlank(rawQuery)) {
            throw new IllegalArgumentException("query 不能为空");
        }
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Tavily 搜索工具当前已禁用");
        }
        if (StrUtil.isBlank(properties.getApiKey())) {
            throw new IllegalStateException("Tavily API Key 未配置");
        }

        long startTime = System.currentTimeMillis();
        String topic = resolveTopic(request);
        ChatToolTrace toolTrace = registerToolTrace(toolContext, ChatToolTrace.builder()
            .toolName("tavily_search")
            .status("RUNNING")
            .inputSummary(rawQuery)
            .topic(topic)
            .build());
        markToolUsed(toolContext, "tavily_search");
        publishThinking(toolContext, "🔍 正在联网搜索: " + rawQuery);

        try {

            String effectiveQuery = buildEffectiveQuery(rawQuery, toolContext);
            if (toolTrace != null) {
                toolTrace.setEffectiveInput(effectiveQuery);
            }

            TavilySearchApiResponse response = restClient.post()
                .uri(properties.getSearchPath())
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(new TavilySearchApiRequest(
                    effectiveQuery,
                    topic,
                    properties.getSearchDepth(),
                    request != null && request.getMaxResults() != null && request.getMaxResults() > 0
                        ? request.getMaxResults()
                        : properties.getMaxResults(),
                    properties.isIncludeAnswer(),
                    properties.isIncludeRawContent()
                ))
                .retrieve()
                .body(TavilySearchApiResponse.class);

            if (response == null) {
                throw new IllegalStateException("Tavily 返回空响应");
            }

            List<SearchReference> references = new ArrayList<>();
            if (response.results() != null) {
                for (TavilyResultItem item : response.results()) {
                    if (StrUtil.isBlank(item.url())) {
                        continue;
                    }
                    references.add(new SearchReference(
                        item.title(),
                        item.url(),
                        StrUtil.isNotBlank(item.content()) ? item.content() : ""
                    ));
                }
            }

            appendReferences(toolContext, references);
            publishThinking(toolContext, "📚 搜索完成，找到 " + references.size() + " 条候选来源");
            completeToolTrace(toolTrace, response, references.size(), startTime);

            return new TavilySearchToolResult(
                effectiveQuery,
                StrUtil.isNotBlank(response.answer()) ? response.answer() : "",
                List.copyOf(references)
            );
        }
        catch (RuntimeException exception) {

            failToolTrace(toolTrace, exception, startTime);
            publishThinking(toolContext, "⚠️ 搜索失败: " + exception.getMessage());
            log.warn("Tavily 搜索失败, query={}", rawQuery, exception);
            throw exception;
        }
    }

    private String buildEffectiveQuery(String query, ToolContext toolContext) {
        if (StrUtil.isBlank(query)) {
            return query;
        }

        return TimeSensitiveQueryHelper.buildEffectiveSearchQuery(query, resolveCurrentDate(toolContext));
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

    private String resolveTopic(TavilySearchRequest request) {

        String requestedTopic = normalizeTopic(request != null ? request.getTopic() : null);
        if (requestedTopic != null) {
            return requestedTopic;
        }

        String configuredTopic = normalizeTopic(properties.getTopic());
        if (configuredTopic != null) {
            return configuredTopic;
        }

        if (StrUtil.isNotBlank(properties.getTopic())) {
            log.warn("Tavily 默认 topic 配置不合法: {}, 自动回退为 general", properties.getTopic());
        }
        return "general";
    }

    private String normalizeTopic(String rawTopic) {
        if (StrUtil.isBlank(rawTopic)) {
            return null;
        }

        String normalized = rawTopic.trim().toLowerCase(Locale.ROOT);
        if (ALLOWED_TOPICS.contains(normalized)) {
            return normalized;
        }

        log.warn("收到不受支持的 Tavily topic: {}, 允许值仅为 {}", rawTopic, ALLOWED_TOPICS);
        return null;
    }

    @SuppressWarnings("unchecked")
    private void appendReferences(ToolContext toolContext, List<SearchReference> references) {
        RunnableConfig config = ToolContextHelper.getConfig(toolContext).orElse(null);
        if (config == null || references.isEmpty()) {
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

    private void completeToolTrace(ChatToolTrace toolTrace,
                                   TavilySearchApiResponse response,
                                   int referenceCount,
                                   long startTime) {
        if (toolTrace == null) {
            return;
        }
        toolTrace.setStatus("COMPLETED");
        toolTrace.setReferenceCount(referenceCount);
        toolTrace.setDurationMs(Math.max(0L, System.currentTimeMillis() - startTime));
        String answer = response == null ? "" : StrUtil.blankToDefault(response.answer(), "");
        if (StrUtil.isNotBlank(answer)) {
            toolTrace.setOutputSummary("联网结果已返回，答案摘要：" + clipText(answer, 160));
            return;
        }
        toolTrace.setOutputSummary("联网结果已返回，候选来源 " + referenceCount + " 条");
    }

    private void failToolTrace(ChatToolTrace toolTrace, RuntimeException exception, long startTime) {
        if (toolTrace == null) {
            return;
        }
        toolTrace.setStatus("FAILED");
        toolTrace.setDurationMs(Math.max(0L, System.currentTimeMillis() - startTime));
        toolTrace.setErrorMessage(exception == null ? "" : StrUtil.blankToDefault(exception.getMessage(), ""));
    }

    private String clipText(String value, int maxLength) {
        if (StrUtil.isBlank(value) || maxLength <= 0) {
            return "";
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    private record TavilySearchApiRequest(
        String query,
        String topic,
        @JsonProperty("search_depth")
        String searchDepth,
        @JsonProperty("max_results")
        int maxResults,
        @JsonProperty("include_answer")
        boolean includeAnswer,
        @JsonProperty("include_raw_content")
        boolean includeRawContent
    ) {
    }

    private record TavilySearchApiResponse(
        String answer,
        List<TavilyResultItem> results
    ) {
    }

    private record TavilyResultItem(
        String title,
        String url,
        String content
    ) {
    }
}
