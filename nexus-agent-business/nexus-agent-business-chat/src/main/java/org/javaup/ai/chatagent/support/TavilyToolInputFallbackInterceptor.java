package org.javaup.ai.chatagent.support;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallExecutionContext;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 支撑组件
 * @author: 阿星不是程序员
 **/

@Component
public class TavilyToolInputFallbackInterceptor extends ToolInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TavilyToolInputFallbackInterceptor.class);
    private static final String TAVILY_TOOL_NAME = "tavily_search";

    private final ObjectMapper objectMapper;

    public TavilyToolInputFallbackInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "tavilyToolInputFallbackInterceptor";
    }

    @Override
    public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
        if (!TAVILY_TOOL_NAME.equals(request.getToolName())) {
            return handler.call(request);
        }

        String normalizedArguments = normalizeArguments(request);
        if (StrUtil.isBlank(normalizedArguments)) {
            log.warn("工具 {} 缺少可用入参，toolCallId={}", request.getToolName(), request.getToolCallId());
            return ToolCallResponse.error(
                request.getToolCallId(),
                request.getToolName(),
                "tavily_search 工具缺少可用的 query 参数"
            );
        }

        if (normalizedArguments.equals(request.getArguments())) {
            return handler.call(request);
        }

        ToolCallRequest.Builder builder = ToolCallRequest.builder(request)
            .arguments(normalizedArguments);
        request.getExecutionContext().ifPresent(builder::executionContext);

        log.warn("工具 {} 收到空或不规范的 arguments，已自动改写为 {}", request.getToolName(), normalizedArguments);
        return handler.call(builder.build());
    }

    private String normalizeArguments(ToolCallRequest request) {
        String arguments = request.getArguments();
        String fallbackQuery = resolveFallbackQuery(request);

        if (StrUtil.isBlank(arguments)) {
            return buildQueryPayload(fallbackQuery);
        }

        try {
            JsonNode rootNode = objectMapper.readTree(arguments);

            if (rootNode != null && rootNode.isObject()) {
                ObjectNode objectNode = ((ObjectNode) rootNode).deepCopy();
                if (StrUtil.isNotBlank(objectNode.path("query").asText())) {
                    return arguments;
                }
                if (StrUtil.isBlank(fallbackQuery)) {
                    return null;
                }
                objectNode.put("query", fallbackQuery);
                return objectMapper.writeValueAsString(objectNode);
            }

            if (rootNode != null && rootNode.isTextual() && StrUtil.isNotBlank(rootNode.asText())) {
                return buildQueryPayload(rootNode.asText().trim());
            }

            return buildQueryPayload(fallbackQuery);
        }
        catch (JsonProcessingException exception) {

            if (StrUtil.isNotBlank(arguments)) {
                return buildQueryPayload(arguments.trim());
            }
            return buildQueryPayload(fallbackQuery);
        }
    }

    private String resolveFallbackQuery(ToolCallRequest request) {

        return request.getExecutionContext()
            .map(ToolCallExecutionContext::config)
            .map(RunnableConfig::context)
            .map(context -> context.get(ChatContextKeys.QUESTION))
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .filter(StrUtil::isNotBlank)
            .map(String::trim)
            .orElse("");
    }

    private String buildQueryPayload(String query) {
        if (StrUtil.isBlank(query)) {
            return null;
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("query", query.trim());

        try {
            return objectMapper.writeValueAsString(payload);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("构造 tavily_search 入参 JSON 失败", exception);
        }
    }
}
