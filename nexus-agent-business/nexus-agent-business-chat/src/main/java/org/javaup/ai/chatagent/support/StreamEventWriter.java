package org.javaup.ai.chatagent.support;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.javaup.ai.chatagent.model.SearchReference;
import org.springframework.stereotype.Component;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 支撑组件
 * @author: 阿星不是程序员
 **/
@Component
public class StreamEventWriter {

    private final ObjectMapper objectMapper;

    public StreamEventWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String text(String content) {
        return text(content, null);
    }

    public String text(String content, StreamEventMetadata metadata) {

        return write(event("text", content, metadata));
    }

    public String thinking(String content) {
        return thinking(content, null);
    }

    public String thinking(String content, StreamEventMetadata metadata) {

        return write(event("thinking", content, metadata));
    }

    public String status(String content) {
        return status(content, null);
    }

    public String status(String content, StreamEventMetadata metadata) {

        return write(event("status", content, metadata));
    }

    public String error(String content) {
        return error(content, null);
    }

    public String error(String content, StreamEventMetadata metadata) {

        return write(event("error", content, metadata));
    }

    public String references(List<SearchReference> references) {
        return references(references, null);
    }

    public String references(List<SearchReference> references, StreamEventMetadata metadata) {

        Map<String, Object> payload = event("reference", references, metadata);
        payload.put("count", references != null ? references.size() : 0);
        return write(payload);
    }

    public String recommendations(List<String> recommendations) {
        return recommendations(recommendations, null);
    }

    public String recommendations(List<String> recommendations, StreamEventMetadata metadata) {

        Map<String, Object> payload = event("recommend", recommendations, metadata);
        payload.put("count", recommendations != null ? recommendations.size() : 0);
        return write(payload);
    }

    private Map<String, Object> event(String type, Object content, StreamEventMetadata metadata) {

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("content", content);
        payload.put("timestamp", Instant.now().toString());
        if (metadata != null) {

            if (metadata.conversationId() != null && !metadata.conversationId().isBlank()) {
                payload.put("conversationId", metadata.conversationId());
            }
            if (metadata.exchangeId() != null && metadata.exchangeId() > 0) {
                payload.put("exchangeId", metadata.exchangeId());
            }
        }
        return payload;
    }

    private String write(Map<String, Object> payload) {

        try {
            return objectMapper.writeValueAsString(payload);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("流式事件序列化失败", exception);
        }
    }
}
