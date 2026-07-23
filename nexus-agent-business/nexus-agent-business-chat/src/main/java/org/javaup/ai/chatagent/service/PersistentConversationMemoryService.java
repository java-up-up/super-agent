package org.javaup.ai.chatagent.service;

import cn.hutool.core.util.StrUtil;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.chatagent.data.SuperAgentChatMemorySummary;
import org.javaup.ai.chatagent.mapper.SuperAgentChatMemorySummaryMapper;
import org.javaup.ai.chatagent.model.ConversationExchangeView;
import org.javaup.ai.chatagent.model.ConversationMemorySummaryView;
import org.javaup.ai.chatagent.model.memory.ConversationMemoryContext;
import org.javaup.ai.chatagent.model.memory.ConversationSummaryPayload;
import org.javaup.ai.chatagent.rag.config.ChatRagProperties;
import org.javaup.ai.prompt.PromptTemplateNames;
import org.javaup.ai.prompt.PromptTemplateService;
import org.javaup.enums.BusinessStatus;
import org.javaup.enums.ChatTurnStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/

@Slf4j
@Service
public class PersistentConversationMemoryService implements ConversationMemoryService {

    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{.*}", Pattern.DOTALL);
    private static final Pattern RETRIEVAL_HINT_PATTERN = Pattern.compile("[a-zA-Z0-9._-]{2,}|[\\p{IsHan}]{2,12}");
    private static final int MAX_SECTION_ITEMS = 6;
    private static final int MAX_ITEM_LENGTH = 80;
    private static final int MAX_GOAL_LENGTH = 120;
    private static final int MAX_QUESTION_LENGTH = 160;
    private static final int MAX_ANSWER_LENGTH = 320;
    private static final int MAX_ANSWER_CONTEXT_ANSWER_LENGTH = 220;

    private final ConversationArchiveStore conversationArchiveStore;
    private final SuperAgentChatMemorySummaryMapper summaryMapper;
    private final ObjectMapper objectMapper;
    private final ChatRagProperties properties;
    private final ExecutorService chatMemorySummaryExecutorService;
    private final ObservedChatModelService observedChatModelService;
    private final PromptTemplateService promptTemplateService;
    private final Set<String> refreshingConversationIds = ConcurrentHashMap.newKeySet();

    @Resource
    private UidGenerator uidGenerator;

    public PersistentConversationMemoryService(ConversationArchiveStore conversationArchiveStore,
                                               SuperAgentChatMemorySummaryMapper summaryMapper,
                                               ObjectMapper objectMapper,
                                               ChatRagProperties properties,
                                               @Qualifier("chatMemorySummaryExecutorService") ExecutorService chatMemorySummaryExecutorService,
                                               ObservedChatModelService observedChatModelService,
                                               PromptTemplateService promptTemplateService) {
        this.conversationArchiveStore = conversationArchiveStore;
        this.summaryMapper = summaryMapper;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.chatMemorySummaryExecutorService = chatMemorySummaryExecutorService;
        this.observedChatModelService = observedChatModelService;
        this.promptTemplateService = promptTemplateService;
    }

    @Override
    public ConversationMemoryContext loadMemoryContext(String conversationId) {
        return loadMemoryContext(conversationId, null);
    }

    @Override
    public ConversationMemoryContext loadMemoryContext(String conversationId, ConversationTraceRecorder traceRecorder) {
        if (StrUtil.isBlank(conversationId)) {
            return emptyContext();
        }

        ChatRagProperties.HistorySummaryProperties historySummaryProperties = properties.getHistorySummary();
        if (!historySummaryProperties.isEnabled()) {

            String recentTranscript = renderRecentTranscript(
                conversationArchiveStore.listRecentExchanges(conversationId, Math.max(1, properties.getRewriteHistoryTurns() * 3)),
                Math.max(1, properties.getRewriteHistoryTurns()),
                historySummaryProperties.getRecentTranscriptMaxChars()
            );
            String answerRecentTranscript = renderAnswerRecentTranscript(
                conversationArchiveStore.listRecentExchanges(conversationId, Math.max(1, properties.getRewriteHistoryTurns() * 3)),
                Math.max(1, properties.getRewriteHistoryTurns()),
                Math.max(1, properties.getAnswerHistoryMaxChars())
            );
            return ConversationMemoryContext.builder()
                .assembledHistory(recentTranscript)
                .longTermSummary("")
                .recentTranscript(recentTranscript)
                .answerRecentTranscript(answerRecentTranscript)
                .summaryPayload(ConversationSummaryPayload.builder().build())
                .coveredExchangeId(0L)
                .coveredExchangeCount(0)
                .compressionCount(0)
                .compressionApplied(false)
                .build();
        }

        SuperAgentChatMemorySummary summaryState = refreshSummaryIfNecessary(
            conversationId,
            findSummary(conversationId).orElse(null),
            traceRecorder
        );
        ConversationSummaryPayload summaryPayload = readSummaryPayload(summaryState);

        List<ConversationExchangeView> recentExchanges = conversationArchiveStore.listRecentExchanges(
            conversationId,
            recentFetchLimit(historySummaryProperties.getKeepRecentTurns())
        );
        String recentTranscript = renderRecentTranscript(
            recentExchanges,
            historySummaryProperties.getKeepRecentTurns(),
            historySummaryProperties.getRecentTranscriptMaxChars()
        );
        String answerRecentTranscript = renderAnswerRecentTranscript(
            recentExchanges,
            historySummaryProperties.getKeepRecentTurns(),
            Math.max(1, properties.getAnswerHistoryMaxChars())
        );
        String longTermSummary = summaryState == null ? "" : safeText(summaryState.getSummaryText());

        return ConversationMemoryContext.builder()

            .assembledHistory(assembleHistory(longTermSummary, recentTranscript))
            .longTermSummary(longTermSummary)
            .recentTranscript(recentTranscript)
            .answerRecentTranscript(answerRecentTranscript)
            .summaryPayload(summaryPayload)
            .coveredExchangeId(summaryState == null ? 0L : defaultLong(summaryState.getCoveredExchangeId()))
            .coveredExchangeCount(summaryState == null ? 0 : safeInt(summaryState.getCoveredExchangeCount()))
            .compressionCount(summaryState == null ? 0 : safeInt(summaryState.getCompressionCount()))
            .compressionApplied(StrUtil.isNotBlank(longTermSummary))
            .build();
    }

    @Override
    public void refreshConversationSummaryAsync(String conversationId) {
        if (StrUtil.isBlank(conversationId) || !properties.getHistorySummary().isEnabled()) {
            return;
        }

        if (!refreshingConversationIds.add(conversationId)) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                refreshSummaryIfNecessary(conversationId, findSummary(conversationId).orElse(null), null);
            }
            catch (Exception exception) {
                log.warn("异步预热会话摘要失败, conversationId={}", conversationId, exception);
            }
            finally {
                refreshingConversationIds.remove(conversationId);
            }
        }, chatMemorySummaryExecutorService);
    }

    @Override
    public ConversationMemorySummaryView getConversationSummary(String conversationId) {
        if (StrUtil.isBlank(conversationId)) {
            return emptySummaryView("");
        }
        return toSummaryView(conversationId, findSummary(conversationId).orElse(null));
    }

    @Override
    public ConversationMemorySummaryView rebuildConversationSummary(String conversationId) {
        if (StrUtil.isBlank(conversationId)) {
            return emptySummaryView("");
        }

        if (!refreshingConversationIds.add(conversationId)) {
            return getConversationSummary(conversationId);
        }
        try {
            summaryMapper.delete(new LambdaQueryWrapper<SuperAgentChatMemorySummary>()
                .eq(SuperAgentChatMemorySummary::getConversationId, conversationId));
            SuperAgentChatMemorySummary rebuiltState = refreshSummaryIfNecessary(conversationId, null, null);
            return toSummaryView(conversationId, rebuiltState);
        }
        finally {
            refreshingConversationIds.remove(conversationId);
        }
    }

    @Override
    public void deleteConversationSummary(String conversationId) {
        if (StrUtil.isBlank(conversationId)) {
            return;
        }
        summaryMapper.delete(new LambdaQueryWrapper<SuperAgentChatMemorySummary>()
            .eq(SuperAgentChatMemorySummary::getConversationId, conversationId));
    }

    private SuperAgentChatMemorySummary refreshSummaryIfNecessary(String conversationId,
                                                                  SuperAgentChatMemorySummary currentState,
                                                                  ConversationTraceRecorder traceRecorder) {
        ChatRagProperties.HistorySummaryProperties historySummaryProperties = properties.getHistorySummary();
        long coveredExchangeId = currentState == null ? 0L : defaultLong(currentState.getCoveredExchangeId());

        List<ConversationExchangeView> incrementalExchanges = conversationArchiveStore.listExchangesAfter(
            conversationId,
            coveredExchangeId
        );
        List<ConversationExchangeView> stableExchanges = incrementalExchanges.stream()
            .filter(this::isStableSummaryExchange)
            .toList();

        int overflowCount = Math.max(0, stableExchanges.size() - historySummaryProperties.getKeepRecentTurns());
        if (overflowCount <= 0) {
            return currentState;
        }

        List<ConversationExchangeView> overflowExchanges = stableExchanges.subList(0, overflowCount);
        SuperAgentChatMemorySummary workingState = currentState;

        for (int start = 0; start < overflowExchanges.size(); start += historySummaryProperties.getCompressionBatchTurns()) {
            int end = Math.min(start + historySummaryProperties.getCompressionBatchTurns(), overflowExchanges.size());
            List<ConversationExchangeView> batch = overflowExchanges.subList(start, end);
            ConversationSummaryPayload mergedPayload = mergeSummaryPayload(readSummaryPayload(workingState), batch, traceRecorder);
            ConversationExchangeView lastExchange = batch.get(batch.size() - 1);
            workingState = saveSummarySnapshot(
                conversationId,
                workingState,
                mergedPayload,
                lastExchange.getExchangeId(),
                safeInt(workingState == null ? null : workingState.getCoveredExchangeCount()) + batch.size(),
                resolveSourceTime(lastExchange)
            );
        }
        return workingState;
    }

    private ConversationSummaryPayload mergeSummaryPayload(ConversationSummaryPayload existingPayload,
                                                           List<ConversationExchangeView> batch,
                                                           ConversationTraceRecorder traceRecorder) {
        try {
            String content = observedChatModelService.callText(
                "summary",
                promptTemplateService.render(PromptTemplateNames.CONVERSATION_SUMMARY_SYSTEM, Map.of()),
                buildSummaryMergePrompt(existingPayload, batch),
                traceRecorder
            );
            ConversationSummaryPayload parsedPayload = parseSummaryPayload(content);
            if (parsedPayload != null) {
                return normalizePayload(parsedPayload);
            }
        }
        catch (Exception exception) {
            log.warn("合并会话长期摘要失败，回退到规则压缩: {}", exception.getMessage());
        }
        return fallbackMerge(existingPayload, batch);
    }

    private String buildSummaryMergePrompt(ConversationSummaryPayload existingPayload,
                                           List<ConversationExchangeView> batch) {
        String existingJson = writePayloadJson(normalizePayload(copyPayload(existingPayload)));
        return promptTemplateService.render(PromptTemplateNames.CONVERSATION_SUMMARY_MERGE, Map.of(
            "existingSummaryJson", StrUtil.isNotBlank(existingJson) ? existingJson : "{}",
            "newConversationBatch", renderCompressionTranscript(batch)
        ));
    }

    private ConversationSummaryPayload fallbackMerge(ConversationSummaryPayload existingPayload,
                                                     List<ConversationExchangeView> batch) {
        ConversationSummaryPayload mergedPayload = copyPayload(existingPayload);
        String batchHighlight = renderFallbackBatchHighlight(batch);
        String mergedSummary = joinNonBlank(mergedPayload.getSummary(), batchHighlight, "；");
        mergedPayload.setSummary(clipText(mergedSummary, properties.getHistorySummary().getSummaryMaxChars()));

        ConversationExchangeView lastExchange = batch.get(batch.size() - 1);
        if (StrUtil.isBlank(mergedPayload.getConversationGoal()) && StrUtil.isNotBlank(lastExchange.getQuestion())) {
            mergedPayload.setConversationGoal(clipText(lastExchange.getQuestion(), MAX_GOAL_LENGTH));
        }

        List<String> pendingQuestions = new ArrayList<>(safeList(mergedPayload.getPendingQuestions()));
        for (ConversationExchangeView exchange : batch) {
            if (StrUtil.isNotBlank(exchange.getQuestion())) {
                pendingQuestions.add(clipText(exchange.getQuestion(), MAX_ITEM_LENGTH));
            }
        }
        mergedPayload.setPendingQuestions(deduplicateAndLimit(pendingQuestions));

        List<String> retrievalHints = new ArrayList<>(safeList(mergedPayload.getRetrievalHints()));
        if (StrUtil.isNotBlank(lastExchange.getQuestion())) {
            retrievalHints.addAll(extractRetrievalHints(lastExchange.getQuestion()));
        }
        mergedPayload.setRetrievalHints(deduplicateAndLimit(retrievalHints));
        return normalizePayload(mergedPayload);
    }

    private SuperAgentChatMemorySummary saveSummarySnapshot(String conversationId,
                                                            SuperAgentChatMemorySummary currentState,
                                                            ConversationSummaryPayload payload,
                                                            long coveredExchangeId,
                                                            int coveredExchangeCount,
                                                            Date lastSourceEditTime) {
        SuperAgentChatMemorySummary latestState = findSummary(conversationId).orElse(null);
        long latestCoveredExchangeId = latestState == null ? 0L : defaultLong(latestState.getCoveredExchangeId());

        if (latestCoveredExchangeId > coveredExchangeId) {
            return latestState;
        }

        if (latestState != null
            && latestCoveredExchangeId == coveredExchangeId
            && StrUtil.isNotBlank(latestState.getSummaryText())) {
            return latestState;
        }

        String summaryText = buildLongTermSummaryText(payload);
        String summaryJson = writePayloadJson(payload);

        if (latestState == null) {
            SuperAgentChatMemorySummary newState = new SuperAgentChatMemorySummary();
            newState.setId(uidGenerator.getUid());
            newState.setConversationId(conversationId);
            newState.setCoveredExchangeId(coveredExchangeId);
            newState.setCoveredExchangeCount(Math.max(coveredExchangeCount, 0));
            newState.setCompressionCount(1);
            newState.setSummaryVersion(1);
            newState.setSummaryText(summaryText);
            newState.setSummaryJson(summaryJson);
            newState.setLastSourceEditTime(lastSourceEditTime);
            newState.setStatus(BusinessStatus.YES.getCode());
            summaryMapper.insert(newState);
            return newState;
        }

        SuperAgentChatMemorySummary updateState = new SuperAgentChatMemorySummary();
        updateState.setId(latestState.getId());
        updateState.setCoveredExchangeId(coveredExchangeId);
        updateState.setCoveredExchangeCount(Math.max(coveredExchangeCount, safeInt(latestState.getCoveredExchangeCount())));
        updateState.setCompressionCount(safeInt(latestState.getCompressionCount()) + 1);
        updateState.setSummaryVersion(safeInt(latestState.getSummaryVersion()) + 1);
        updateState.setSummaryText(summaryText);
        updateState.setSummaryJson(summaryJson);
        updateState.setLastSourceEditTime(lastSourceEditTime);
        summaryMapper.updateById(updateState);

        latestState.setCoveredExchangeId(updateState.getCoveredExchangeId());
        latestState.setCoveredExchangeCount(updateState.getCoveredExchangeCount());
        latestState.setCompressionCount(updateState.getCompressionCount());
        latestState.setSummaryVersion(updateState.getSummaryVersion());
        latestState.setSummaryText(updateState.getSummaryText());
        latestState.setSummaryJson(updateState.getSummaryJson());
        latestState.setLastSourceEditTime(updateState.getLastSourceEditTime());
        return latestState;
    }

    private Optional<SuperAgentChatMemorySummary> findSummary(String conversationId) {
        return Optional.ofNullable(summaryMapper.selectOne(
            new LambdaQueryWrapper<SuperAgentChatMemorySummary>()
                .eq(SuperAgentChatMemorySummary::getConversationId, conversationId)
                .orderByDesc(SuperAgentChatMemorySummary::getId)
                .last("LIMIT 1")
        ));
    }

    private ConversationSummaryPayload readSummaryPayload(SuperAgentChatMemorySummary summaryState) {
        if (summaryState == null) {
            return ConversationSummaryPayload.builder().build();
        }
        if (StrUtil.isNotBlank(summaryState.getSummaryJson())) {
            ConversationSummaryPayload payload = parseSummaryPayload(summaryState.getSummaryJson());
            if (payload != null) {
                return normalizePayload(payload);
            }
        }
        ConversationSummaryPayload fallbackPayload = ConversationSummaryPayload.builder()
            .summary(summaryState.getSummaryText())
            .build();
        return normalizePayload(fallbackPayload);
    }

    private ConversationSummaryPayload parseSummaryPayload(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(extractJsonObject(raw));
            ConversationSummaryPayload payload = ConversationSummaryPayload.builder()
                .summary(root.path("summary").asText(""))
                .conversationGoal(root.path("conversation_goal").asText(""))
                .stableFacts(readStringArray(root.path("stable_facts")))
                .userPreferences(readStringArray(root.path("user_preferences")))
                .resolvedPoints(readStringArray(root.path("resolved_points")))
                .pendingQuestions(readStringArray(root.path("pending_questions")))
                .retrievalHints(readStringArray(root.path("retrieval_hints")))
                .build();
            return normalizePayload(payload);
        }
        catch (Exception exception) {
            log.debug("解析会话长期摘要 JSON 失败: {}", raw, exception);
            return null;
        }
    }

    private List<String> readStringArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        node.forEach(item -> {
            String text = item.asText("").trim();
            if (StrUtil.isNotBlank(text)) {
                result.add(text);
            }
        });
        return result;
    }

    private String renderCompressionTranscript(List<ConversationExchangeView> batch) {
        StringBuilder builder = new StringBuilder();
        for (ConversationExchangeView exchange : batch) {
            if (StrUtil.isNotBlank(exchange.getQuestion())) {
                builder.append("用户：")
                    .append(clipText(exchange.getQuestion(), MAX_QUESTION_LENGTH))
                    .append('\n');
            }
            if (StrUtil.isNotBlank(exchange.getAnswer())) {
                builder.append("助手：")
                    .append(clipText(exchange.getAnswer(), MAX_ANSWER_LENGTH))
                    .append('\n');
            }
            if (exchange.getStatus() == ChatTurnStatus.STOPPED && StrUtil.isNotBlank(exchange.getErrorMessage())) {
                builder.append("补充说明：本轮被停止，说明=")
                    .append(clipText(exchange.getErrorMessage(), MAX_ITEM_LENGTH))
                    .append('\n');
            }
        }
        return builder.toString().trim();
    }

    private String renderFallbackBatchHighlight(List<ConversationExchangeView> batch) {
        List<String> highlights = new ArrayList<>();
        for (ConversationExchangeView exchange : batch) {
            if (StrUtil.isNotBlank(exchange.getQuestion())) {
                highlights.add("用户关注：" + clipText(exchange.getQuestion(), MAX_ITEM_LENGTH));
            }
            if (StrUtil.isNotBlank(exchange.getAnswer())) {
                highlights.add("已有结论：" + clipText(exchange.getAnswer(), MAX_ITEM_LENGTH));
            }
            if (highlights.size() >= 4) {
                break;
            }
        }
        return String.join("；", highlights);
    }

    private String renderRecentTranscript(List<ConversationExchangeView> exchanges,
                                          int keepRecentTurns,
                                          int recentTranscriptMaxChars) {
        List<ConversationExchangeView> renderableExchanges = new ArrayList<>();
        for (ConversationExchangeView exchange : exchanges) {
            if (shouldKeepInRecentWindow(exchange)) {
                renderableExchanges.add(exchange);
            }
        }
        if (renderableExchanges.isEmpty()) {
            return "";
        }
        int fromIndex = Math.max(0, renderableExchanges.size() - keepRecentTurns);
        StringBuilder builder = new StringBuilder("【最近对话原文】\n");
        for (int index = fromIndex; index < renderableExchanges.size(); index++) {
            ConversationExchangeView exchange = renderableExchanges.get(index);
            if (StrUtil.isNotBlank(exchange.getQuestion())) {
                builder.append("用户：")
                    .append(clipText(exchange.getQuestion(), MAX_QUESTION_LENGTH))
                    .append('\n');
            }
            if (exchange.getStatus() == ChatTurnStatus.COMPLETED && StrUtil.isNotBlank(exchange.getAnswer())) {
                builder.append("助手：")
                    .append(clipText(exchange.getAnswer(), MAX_ANSWER_LENGTH))
                    .append('\n');
            }
        }

        return clipRecentTranscript(builder.toString().trim(), recentTranscriptMaxChars);
    }

    private String renderAnswerRecentTranscript(List<ConversationExchangeView> exchanges,
                                                int keepRecentTurns,
                                                int maxChars) {
        if (exchanges == null || exchanges.isEmpty()) {
            return "";
        }
        List<ConversationExchangeView> renderableExchanges = new ArrayList<>();
        for (ConversationExchangeView exchange : exchanges) {
            if (exchange != null
                && exchange.getStatus() != ChatTurnStatus.RUNNING
                && StrUtil.isNotBlank(exchange.getQuestion())) {
                renderableExchanges.add(exchange);
            }
        }
        if (renderableExchanges.isEmpty()) {
            return "";
        }
        int fromIndex = Math.max(0, renderableExchanges.size() - keepRecentTurns);
        StringBuilder builder = new StringBuilder("【最近相关对话】\n");
        for (int index = fromIndex; index < renderableExchanges.size(); index++) {
            ConversationExchangeView exchange = renderableExchanges.get(index);
            if (StrUtil.isNotBlank(exchange.getQuestion())) {
                builder.append("用户：")
                    .append(clipText(exchange.getQuestion(), MAX_QUESTION_LENGTH))
                    .append('\n');
            }

        }
        return clipRecentTranscript(builder.toString().trim(), maxChars);
    }

    private String buildLongTermSummaryText(ConversationSummaryPayload payload) {
        ConversationSummaryPayload normalizedPayload = normalizePayload(payload);
        StringBuilder builder = new StringBuilder();
        appendSection(builder, "长期会话摘要", normalizedPayload.getSummary());
        appendSection(builder, "会话目标", normalizedPayload.getConversationGoal());
        appendBulletSection(builder, "已确认事实", normalizedPayload.getStableFacts());
        appendBulletSection(builder, "用户偏好与约束", normalizedPayload.getUserPreferences());
        appendBulletSection(builder, "已解决问题", normalizedPayload.getResolvedPoints());
        appendBulletSection(builder, "待跟进问题", normalizedPayload.getPendingQuestions());
        appendBulletSection(builder, "检索提示", normalizedPayload.getRetrievalHints());
        return clipText(builder.toString().trim(), properties.getHistorySummary().getSummaryMaxChars());
    }

    private String assembleHistory(String longTermSummary, String recentTranscript) {
        return joinNonBlank(longTermSummary, recentTranscript, "\n\n").trim();
    }

    private ConversationSummaryPayload normalizePayload(ConversationSummaryPayload payload) {
        ConversationSummaryPayload workingPayload = payload == null ? ConversationSummaryPayload.builder().build() : payload;
        String normalizedSummary = clipText(safeText(workingPayload.getSummary()), properties.getHistorySummary().getSummaryMaxChars());
        if (StrUtil.isBlank(normalizedSummary)) {
            normalizedSummary = synthesizeSummaryFromSections(workingPayload);
        }
        return ConversationSummaryPayload.builder()
            .summary(normalizedSummary)
            .conversationGoal(clipText(safeText(workingPayload.getConversationGoal()), MAX_GOAL_LENGTH))
            .stableFacts(deduplicateAndLimit(workingPayload.getStableFacts()))
            .userPreferences(deduplicateAndLimit(workingPayload.getUserPreferences()))
            .resolvedPoints(deduplicateAndLimit(workingPayload.getResolvedPoints()))
            .pendingQuestions(deduplicateAndLimit(workingPayload.getPendingQuestions()))
            .retrievalHints(deduplicateAndLimit(workingPayload.getRetrievalHints()))
            .build();
    }

    private String synthesizeSummaryFromSections(ConversationSummaryPayload payload) {
        List<String> parts = new ArrayList<>();
        if (StrUtil.isNotBlank(payload.getConversationGoal())) {
            parts.add("目标：" + clipText(payload.getConversationGoal(), MAX_ITEM_LENGTH));
        }
        if (!safeList(payload.getStableFacts()).isEmpty()) {
            parts.add("事实：" + String.join("；", safeList(payload.getStableFacts())));
        }
        if (!safeList(payload.getPendingQuestions()).isEmpty()) {
            parts.add("待跟进：" + String.join("；", safeList(payload.getPendingQuestions())));
        }
        return clipText(String.join("；", parts), properties.getHistorySummary().getSummaryMaxChars());
    }

    private List<String> deduplicateAndLimit(List<String> values) {
        LinkedHashSet<String> deduplicated = new LinkedHashSet<>();
        for (String value : safeList(values)) {
            String text = clipText(safeText(value), MAX_ITEM_LENGTH);
            if (StrUtil.isNotBlank(text)) {
                deduplicated.add(text);
            }
            if (deduplicated.size() >= MAX_SECTION_ITEMS) {
                break;
            }
        }
        return new ArrayList<>(deduplicated);
    }

    private ConversationSummaryPayload copyPayload(ConversationSummaryPayload payload) {
        if (payload == null) {
            return ConversationSummaryPayload.builder().build();
        }
        return ConversationSummaryPayload.builder()
            .summary(payload.getSummary())
            .conversationGoal(payload.getConversationGoal())
            .stableFacts(new ArrayList<>(safeList(payload.getStableFacts())))
            .userPreferences(new ArrayList<>(safeList(payload.getUserPreferences())))
            .resolvedPoints(new ArrayList<>(safeList(payload.getResolvedPoints())))
            .pendingQuestions(new ArrayList<>(safeList(payload.getPendingQuestions())))
            .retrievalHints(new ArrayList<>(safeList(payload.getRetrievalHints())))
            .build();
    }

    private String writePayloadJson(ConversationSummaryPayload payload) {
        try {
            return objectMapper.writeValueAsString(normalizePayload(payload));
        }
        catch (Exception exception) {
            throw new IllegalStateException("序列化会话长期摘要失败", exception);
        }
    }

    private String extractJsonObject(String raw) {
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(raw.trim());
        if (matcher.find()) {
            return matcher.group();
        }
        return raw.trim();
    }

    private boolean isStableSummaryExchange(ConversationExchangeView exchange) {
        if (exchange == null || exchange.getStatus() == null) {
            return false;
        }

        return exchange.getStatus() == ChatTurnStatus.COMPLETED
            && StrUtil.isNotBlank(exchange.getQuestion());
    }

    private boolean shouldKeepInRecentWindow(ConversationExchangeView exchange) {
        return exchange != null
            && exchange.getStatus() != ChatTurnStatus.RUNNING
            && (StrUtil.isNotBlank(exchange.getQuestion()) || StrUtil.isNotBlank(exchange.getAnswer()));
    }

    private int recentFetchLimit(int keepRecentTurns) {

        return Math.max(keepRecentTurns * 3, keepRecentTurns + 4);
    }

    private void appendSection(StringBuilder builder, String title, String content) {
        if (StrUtil.isBlank(content)) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append('【').append(title).append("】\n")
            .append(content.trim())
            .append('\n');
    }

    private void appendBulletSection(StringBuilder builder, String title, List<String> values) {
        List<String> normalizedValues = safeList(values);
        if (normalizedValues.isEmpty()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append('【').append(title).append("】\n");
        for (String value : normalizedValues) {
            builder.append("- ").append(value).append('\n');
        }
    }

    private String clipText(String text, int maxChars) {
        String normalized = safeText(text);
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxChars - 1)) + "…";
    }

    private String clipRecentTranscript(String text, int maxChars) {
        String normalized = safeText(text);
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        int startIndex = Math.max(0, normalized.length() - Math.max(0, maxChars - 1));
        return "…" + normalized.substring(startIndex);
    }

    private String joinNonBlank(String left, String right, String delimiter) {
        if (StrUtil.isBlank(left)) {
            return safeText(right);
        }
        if (StrUtil.isBlank(right)) {
            return safeText(left);
        }
        return safeText(left) + delimiter + safeText(right);
    }

    private List<String> safeList(List<String> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private Date resolveSourceTime(ConversationExchangeView exchange) {
        if (exchange == null) {
            return null;
        }
        return exchange.getEditTime() != null ? exchange.getEditTime() : exchange.getCreateTime();
    }

    private Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }

    private ConversationMemoryContext emptyContext() {
        return ConversationMemoryContext.builder()
            .assembledHistory("")
            .longTermSummary("")
            .recentTranscript("")
            .answerRecentTranscript("")
            .summaryPayload(ConversationSummaryPayload.builder().build())
            .coveredExchangeId(0L)
            .coveredExchangeCount(0)
            .compressionCount(0)
            .compressionApplied(false)
            .build();
    }

    private ConversationMemorySummaryView toSummaryView(String conversationId, SuperAgentChatMemorySummary summaryState) {
        if (summaryState == null) {
            return emptySummaryView(conversationId);
        }
        return new ConversationMemorySummaryView(
            conversationId,
            StrUtil.isNotBlank(summaryState.getSummaryText()),
            defaultLong(summaryState.getCoveredExchangeId()),
            safeInt(summaryState.getCoveredExchangeCount()),
            safeInt(summaryState.getCompressionCount()),
            safeInt(summaryState.getSummaryVersion()),
            safeText(summaryState.getSummaryText()),
            readSummaryPayload(summaryState),
            toInstant(summaryState.getLastSourceEditTime()),
            toInstant(summaryState.getEditTime())
        );
    }

    private ConversationMemorySummaryView emptySummaryView(String conversationId) {
        return new ConversationMemorySummaryView(
            conversationId,
            false,
            0L,
            0,
            0,
            0,
            "",
            ConversationSummaryPayload.builder().build(),
            null,
            null
        );
    }

    private List<String> extractRetrievalHints(String question) {
        if (StrUtil.isBlank(question)) {
            return List.of();
        }
        LinkedHashSet<String> hints = new LinkedHashSet<>();
        Matcher matcher = RETRIEVAL_HINT_PATTERN.matcher(question);
        while (matcher.find()) {
            String hint = matcher.group().trim();
            if (hint.length() >= 2 && !isNoiseHint(hint)) {
                hints.add(clipText(hint, MAX_ITEM_LENGTH));
            }
            if (hints.size() >= MAX_SECTION_ITEMS) {
                break;
            }
        }
        return new ArrayList<>(hints);
    }

    private boolean isNoiseHint(String value) {
        return "请问".equals(value)
            || "帮我".equals(value)
            || "一下".equals(value)
            || "如何".equals(value)
            || "怎么".equals(value)
            || "什么".equals(value)
            || "哪个".equals(value)
            || "这个".equals(value)
            || "那个".equals(value)
            || "可以".equals(value)
            || "需要".equals(value);
    }
}
