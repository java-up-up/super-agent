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
import org.javaup.ai.chatagent.config.YoucomResearchProperties;
import org.javaup.ai.chatagent.model.debug.ChatDebugTrace;
import org.javaup.ai.chatagent.model.debug.ChatToolTrace;
import org.javaup.ai.chatagent.support.ChatContextKeys;
import org.javaup.ai.chatagent.support.RestClientFactorySupport;
import org.javaup.ai.chatagent.support.SinkEmitHelper;
import org.javaup.ai.chatagent.support.StreamEventMetadata;
import org.javaup.ai.chatagent.support.StreamEventWriter;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: You.com Research API 工具
 * @author: Claude
 **/
@Slf4j
@Component
public class YoucomResearchTool {

    private static final Set<String> ALLOWED_EFFORTS = Set.of("lite", "standard", "deep", "exhaustive");

    private final YoucomResearchProperties properties;
    private final StreamEventWriter streamEventWriter;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public YoucomResearchTool(YoucomResearchProperties properties, StreamEventWriter streamEventWriter) {
        this.properties = properties;
        this.streamEventWriter = streamEventWriter;
        this.restClient = RestClientFactorySupport.create(
            properties.getBaseUrl(),
            properties.getConnectTimeoutMs(),
            properties.getReadTimeoutMs()
        );
    }

    public YoucomResearchToolResult research(YoucomResearchRequest request, ToolContext toolContext) {

        String rawInput = request != null && StrUtil.isNotBlank(request.getInput()) ? request.getInput().trim() : "";
        if (StrUtil.isBlank(rawInput)) {
            throw new IllegalArgumentException("input 不能为空");
        }
        if (!properties.isEnabled()) {
            throw new IllegalStateException("You.com 研究工具当前已禁用");
        }
        if (StrUtil.isBlank(properties.getApiKey())) {
            throw new IllegalStateException("You.com API Key 未配置");
        }

        long startTime = System.currentTimeMillis();
        String effort = resolveEffort(request);
        ChatToolTrace toolTrace = registerToolTrace(toolContext, ChatToolTrace.builder()
            .toolName("youcom_research")
            .status("RUNNING")
            .inputSummary(rawInput)
            .build());
        markToolUsed(toolContext, "youcom_research");
        publishThinking(toolContext, "🔬 正在通过 You.com 进行深度研究: " + rawInput);

        try {
            YoucomResearchApiRequest apiRequest = new YoucomResearchApiRequest(rawInput, effort);

            String responseBody = restClient.post()
                .uri(properties.getResearchPath())
                .header("X-API-Key", properties.getApiKey())
                .header("Content-Type", "application/json")
                .body(apiRequest)
                .retrieve()
                .body(String.class);

            YoucomResearchToolResult result = parseResult(responseBody);

            publishThinking(toolContext, "📝 研究完成，生成答案约 " +
                (result.getContent() != null ? result.getContent().length() : 0) + " 字");
            completeToolTrace(toolTrace, startTime);

            return result;
        }
        catch (RuntimeException exception) {
            failToolTrace(toolTrace, exception, startTime);
            publishThinking(toolContext, "⚠️ 研究失败: " + exception.getMessage());
            log.warn("You.com 研究失败, input={}", rawInput, exception);
            throw exception;
        }
    }

    private String resolveEffort(YoucomResearchRequest request) {
        String requested = request != null ? request.getResearchEffort() : null;
        if (StrUtil.isNotBlank(requested)) {
            String normalized = requested.trim().toLowerCase();
            if (ALLOWED_EFFORTS.contains(normalized)) {
                return normalized;
            }
            log.warn("收到不受支持的 research_effort: {}，允许值: {}，自动回退为 standard", requested, ALLOWED_EFFORTS);
        }
        String configured = properties.getResearchEffort();
        if (StrUtil.isNotBlank(configured) && ALLOWED_EFFORTS.contains(configured.toLowerCase())) {
            return configured.toLowerCase();
        }
        return "standard";
    }

    private YoucomResearchToolResult parseResult(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("content").asText("");
            List<YoucomResearchToolResult.YoucomResearchSource> sources = new ArrayList<>();
            JsonNode sourcesNode = root.path("sources");
            if (sourcesNode.isArray()) {
                for (JsonNode source : sourcesNode) {
                    List<String> snippets = new ArrayList<>();
                    JsonNode snippetsNode = source.path("snippets");
                    if (snippetsNode.isArray()) {
                        for (JsonNode s : snippetsNode) {
                            snippets.add(s.asText());
                        }
                    }
                    sources.add(new YoucomResearchToolResult.YoucomResearchSource(
                        source.path("url").asText(""),
                        source.path("title").asText(""),
                        snippets
                    ));
                }
            }
            return new YoucomResearchToolResult(content, sources);
        }
        catch (Exception e) {
            log.warn("解析 You.com 研究响应失败: {}", e.getMessage());
            return new YoucomResearchToolResult("", List.of());
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

    private void completeToolTrace(ChatToolTrace toolTrace, long startTime) {
        if (toolTrace == null) {
            return;
        }
        toolTrace.setStatus("COMPLETED");
        toolTrace.setDurationMs(Math.max(0L, System.currentTimeMillis() - startTime));
        toolTrace.setOutputSummary("You.com 研究答案已生成");
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
    private static class YoucomResearchApiRequest {
        private String input;
        @JsonProperty("research_effort")
        private String researchEffort;
    }
}
