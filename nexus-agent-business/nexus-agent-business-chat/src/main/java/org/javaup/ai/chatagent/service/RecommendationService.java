package org.javaup.ai.chatagent.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.javaup.ai.chatagent.config.ChatAgentProperties;
import org.javaup.ai.chatagent.model.ConversationExchangeView;
import org.javaup.ai.prompt.PromptTemplateNames;
import org.javaup.ai.prompt.PromptTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final ChatAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final ExecutorService recommendationExecutorService;
    private final ObservedChatModelService observedChatModelService;
    private final PromptTemplateService promptTemplateService;

    public RecommendationService(ChatAgentProperties properties,
                                 ObjectMapper objectMapper,
                                 @Qualifier("chatPostProcessExecutorService") ExecutorService recommendationExecutorService,
                                 ObservedChatModelService observedChatModelService,
                                 PromptTemplateService promptTemplateService) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.recommendationExecutorService = recommendationExecutorService;
        this.observedChatModelService = observedChatModelService;
        this.promptTemplateService = promptTemplateService;
    }

    public List<String> generateRecommendations(String question,
                                                String answer,
                                                List<ConversationExchangeView> recentExchanges,
                                                ConversationTraceRecorder traceRecorder) {

        if (!properties.isRecommendationEnabled() || StrUtil.isBlank(answer)) {
            return List.of();
        }

        try {
            return CompletableFuture.supplyAsync(
                    () -> generateRecommendationsInternal(question, answer, recentExchanges, traceRecorder),

                    recommendationExecutorService
                )

                .orTimeout(Math.max(properties.getRecommendationTimeoutMs(), 1L), TimeUnit.MILLISECONDS)
                .exceptionally(exception -> {
                    log.warn("生成推荐问题超时或失败: {}", exception.getMessage());
                    return List.of();
                })
                .join();
        }
        catch (Exception exception) {
            log.warn("生成推荐问题失败", exception);
            return List.of();
        }
    }

    private List<String> generateRecommendationsInternal(String question,
                                                         String answer,
                                                         List<ConversationExchangeView> recentExchanges,
                                                         ConversationTraceRecorder traceRecorder) {

        List<ConversationExchangeView> safeRecentExchanges = recentExchanges == null ? List.of() : recentExchanges;
        StringBuilder recentContext = new StringBuilder();

        int startIndex = Math.max(0, safeRecentExchanges.size() - properties.getHistoryPreviewTurns());

        for (int index = startIndex; index < safeRecentExchanges.size(); index++) {
            ConversationExchangeView exchange = safeRecentExchanges.get(index);
            recentContext.append("用户：").append(exchange.getQuestion()).append('\n');
            if (StrUtil.isNotBlank(exchange.getAnswer())) {
                recentContext.append("助手：").append(exchange.getAnswer()).append('\n');
            }
        }

        String prompt = promptTemplateService.render(PromptTemplateNames.RECOMMENDATION_USER, Map.of(
            "basePrompt", StrUtil.blankToDefault(properties.getRecommendationPrompt(), ""),
            "recentContext", recentContext.toString().trim(),
            "question", StrUtil.blankToDefault(question, ""),
            "answer", StrUtil.blankToDefault(answer, "")
        ));

        try {

            String content = observedChatModelService.callText("recommendation", null, prompt, traceRecorder);

            if (StrUtil.isBlank(content)) {
                return List.of();
            }

            String jsonArray = extractJsonArray(content);
            if (StrUtil.isBlank(jsonArray)) {
                log.warn("推荐问题输出不是有效 JSON 数组: {}", content);
                return List.of();
            }

            List<String> rawList = objectMapper.readValue(jsonArray, new TypeReference<List<String>>() {
            });
            LinkedHashSet<String> unique = new LinkedHashSet<>();

            for (String item : rawList) {
                if (StrUtil.isNotBlank(item)) {
                    unique.add(item.trim());
                }
                if (unique.size() >= 3) {
                    break;
                }
            }
            return new ArrayList<>(unique);
        }
        catch (Exception exception) {
            log.warn("生成推荐问题失败", exception);
            return List.of();
        }
    }

    private String extractJsonArray(String content) {

        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return null;
        }
        return content.substring(start, end + 1);
    }
}
