package org.javaup.ai.chatagent.rag.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.chatagent.rag.config.ChatRagProperties;
import org.javaup.ai.chatagent.rag.model.RagRewriteResult;
import org.javaup.ai.chatagent.service.ConversationTraceRecorder;
import org.javaup.ai.chatagent.service.ObservedChatModelService;
import org.javaup.ai.prompt.PromptTemplateNames;
import org.javaup.ai.prompt.PromptTemplateService;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/

@Slf4j
@Service
public class ChatQueryRewriteService {

    private static final Pattern NUMBERED_MULTI_QUESTION_PATTERN = Pattern.compile("(^|\\s)(\\d+[)\\.、]|[A-Za-z][)])");
    private static final Pattern MULTI_LINE_PATTERN = Pattern.compile("\\n+");

    private final ObservedChatModelService observedChatModelService;
    private final ObjectMapper objectMapper;
    private final ChatRagProperties properties;
    private final PromptTemplateService promptTemplateService;

    public ChatQueryRewriteService(ObservedChatModelService observedChatModelService,
                                   ObjectMapper objectMapper,
                                   ChatRagProperties properties,
                                   PromptTemplateService promptTemplateService) {
        this.observedChatModelService = observedChatModelService;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.promptTemplateService = promptTemplateService;
    }

    public RagRewriteResult rewrite(String question, String historySummary) {
        return rewrite(question, historySummary, null);
    }

    public RagRewriteResult rewrite(String question,
                                    String historySummary,
                                    ConversationTraceRecorder traceRecorder) {
        String normalizedQuestion = StrUtil.trim(question);
        if (StrUtil.isBlank(normalizedQuestion)) {
            return new RagRewriteResult("", List.of());
        }
        if (!properties.isRewriteEnabled() || !needsRewrite(normalizedQuestion, historySummary)) {
            RagRewriteResult fallback = fallback(normalizedQuestion);
            log.info("RAG 改写跳过: question='{}', rewritten='{}', subQuestions={}",
                normalizedQuestion,
                fallback.getRewrittenQuestion(),
                fallback.getSubQuestions());
            return fallback;
        }
        try {
            String prompt = promptTemplateService.render(PromptTemplateNames.CHAT_QUERY_REWRITE, Map.of(
                "history", StrUtil.isNotBlank(historySummary) ? historySummary : "无历史上下文",
                "question", normalizedQuestion
            ));
            String raw = observedChatModelService.callText("rewrite", null, prompt, buildRewriteCallOptions(), traceRecorder);
            RagRewriteResult parsed = normalizeRewriteResult(normalizedQuestion, parse(raw));
            if (parsed != null && StrUtil.isNotBlank(parsed.getRewrittenQuestion())) {
                parsed.setRawModelOutput(raw);
                log.info("RAG 改写完成: question='{}', rewritten='{}', subQuestions={}",
                    normalizedQuestion,
                    parsed.getRewrittenQuestion(),
                    parsed.getSubQuestions());
                return parsed;
            }
            log.warn("RAG 改写结果不可用，回退到规则改写: question='{}', raw='{}'",
                normalizedQuestion,
                StrUtil.blankToDefault(raw, ""));
        }
        catch (Exception exception) {
            log.warn("RAG 改写失败，回退到规则改写: question='{}', message={}",
                normalizedQuestion,
                exception.getMessage());
        }
        return fallback(normalizedQuestion);
    }

    private ChatOptions buildRewriteCallOptions() {
        ChatRagProperties.RewriteOptionsProperties rewriteOptions = properties.getRewriteOptions();
        if (rewriteOptions == null || !rewriteOptions.isEnabled()) {
            log.info("RAG 改写模型参数: overrideEnabled=false, useDefaultModelOptions=true");
            return null;
        }
        log.info("RAG 改写模型参数: overrideEnabled=true, temperature={}, topP={}, thinking={}",
            rewriteOptions.getTemperature(),
            rewriteOptions.getTopP(),
            rewriteOptions.getThinking());
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder();
        if (rewriteOptions.getTemperature() != null) {
            builder.temperature(rewriteOptions.getTemperature());
        }
        if (rewriteOptions.getTopP() != null) {
            builder.topP(rewriteOptions.getTopP());
        }
        if (rewriteOptions.getThinking() != null) {
            builder.extraBody(Map.of("thinking", rewriteOptions.getThinking()));
        }
        return builder.build();
    }

    private RagRewriteResult fallback(String normalizedQuestion) {
        if (!looksLikeExplicitMultiQuestion(normalizedQuestion)) {
            return new RagRewriteResult(normalizedQuestion, List.of(normalizedQuestion));
        }
        return new RagRewriteResult(normalizedQuestion, ruleBasedSplit(normalizedQuestion));
    }

    private boolean needsRewrite(String question, String historySummary) {
        if (StrUtil.isBlank(historySummary)) {
            return question.length() < 8 || looksLikeExplicitMultiQuestion(question);
        }
        return question.length() < 18 || looksLikeExplicitMultiQuestion(question);
    }

    private boolean looksLikeExplicitMultiQuestion(String question) {
        String normalized = StrUtil.blankToDefault(question, "").trim();
        if (normalized.isBlank()) {
            return false;
        }
        long questionMarkCount = normalized.chars().filter(ch -> ch == '?' || ch == '？').count();
        if (questionMarkCount >= 2) {
            return true;
        }
        if (normalized.contains("；") || normalized.contains(";")) {
            return true;
        }
        if (MULTI_LINE_PATTERN.matcher(normalized).find()) {
            long nonBlankLineCount = Arrays.stream(normalized.split("\\n+"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .count();
            if (nonBlankLineCount >= 2) {
                return true;
            }
        }
        if (NUMBERED_MULTI_QUESTION_PATTERN.matcher(normalized).find()) {
            return true;
        }
        return normalized.contains("分别");
    }

    private RagRewriteResult normalizeRewriteResult(String originalQuestion,
                                                    ParsedRewritePayload parsed) {
        if (parsed == null) {
            return null;
        }
        String rewrite = StrUtil.blankToDefault(parsed.rewrite(), originalQuestion).trim();
        if (StrUtil.isBlank(rewrite)) {
            return null;
        }
        List<String> subQuestions = parsed.subQuestions() == null
            ? new ArrayList<>()
            : parsed.subQuestions().stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        boolean explicitMultiQuestion = looksLikeExplicitMultiQuestion(originalQuestion);
        boolean shouldSplit = Boolean.TRUE.equals(parsed.shouldSplit());
        if (!shouldSplit || !explicitMultiQuestion) {
            if (shouldSplit && !explicitMultiQuestion && subQuestions.size() > 1) {
                log.info("RAG 改写子问题收敛: question='{}', rewrite='{}', originalSubQuestionCount={}, reason='llm-split-rejected-by-conservative-structure-check'",
                    originalQuestion,
                    rewrite,
                    subQuestions.size());
            }
            subQuestions = List.of(rewrite);
        }
        else if (subQuestions.isEmpty()) {
            List<String> fallbackSplit = ruleBasedSplit(originalQuestion);
            if (fallbackSplit.size() > 1) {
                subQuestions = fallbackSplit;
            }
            else {
                subQuestions = List.of(rewrite);
            }
        }
        if (subQuestions.size() == 1 && !StrUtil.equals(subQuestions.get(0), rewrite) && !shouldSplit) {
            subQuestions = List.of(rewrite);
        }
        if (subQuestions.size() > properties.getMaxSubQuestions()) {
            subQuestions = subQuestions.subList(0, properties.getMaxSubQuestions());
        }
        return new RagRewriteResult(rewrite, subQuestions);
    }

    private ParsedRewritePayload parse(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(raw.trim());
            String rewrite = root.path("rewrite").asText("").trim();
            if (StrUtil.isBlank(rewrite)) {
                return null;
            }
            Boolean shouldSplit = root.has("should_split") && !root.get("should_split").isNull()
                ? root.path("should_split").asBoolean(false)
                : null;
            List<String> parsedSubQuestions = new ArrayList<>();
            JsonNode subQuestionNode = root.path("sub_questions");
            if (subQuestionNode.isArray()) {
                subQuestionNode.forEach(item -> {
                    String text = item.asText("").trim();
                    if (StrUtil.isNotBlank(text)) {
                        parsedSubQuestions.add(text);
                    }
                });
            }
            return new ParsedRewritePayload(rewrite, shouldSplit, parsedSubQuestions);
        }
        catch (Exception exception) {
            log.warn("解析问题改写结果失败，raw={}", raw, exception);
            return null;
        }
    }

    private List<String> ruleBasedSplit(String question) {
        List<String> result = Arrays.stream(question.split("[?？；;\\n]+"))
            .map(String::trim)
            .filter(StrUtil::isNotBlank)
            .limit(properties.getMaxSubQuestions())
            .toList();
        if (result.isEmpty()) {
            return List.of(question);
        }
        return new ArrayList<>(new LinkedHashSet<>(result));
    }

    private record ParsedRewritePayload(String rewrite, Boolean shouldSplit, List<String> subQuestions) {
    }
}
