package org.javaup.ai.chatagent.rag.service;

import cn.hutool.core.util.StrUtil;
import org.javaup.ai.chatagent.rag.config.ChatRagProperties;
import org.javaup.ai.chatagent.rag.model.AnswerHistoryContext;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/

@Service
public class AnswerHistoryContextAssembler {

    private static final Set<String> FOLLOW_UP_HINTS = Set.of(
        "刚才", "上面", "前面", "前文", "上一条", "上一个", "上一轮", "这个", "那个", "这条", "那条",
        "继续", "展开", "补充", "详细", "细说", "进一步", "为什么", "怎么做", "怎么理解", "还有呢"
    );

    private final ChatRagProperties properties;

    public AnswerHistoryContextAssembler(ChatRagProperties properties) {
        this.properties = properties;
    }

    public AnswerHistoryContext assemble(String question, String answerRecentTranscript) {
        String normalizedQuestion = safeText(question);
        String recentUserContext = extractRecentUserQuestions(answerRecentTranscript);
        int totalBudget = Math.max(1, properties.getAnswerHistoryMaxChars());
        boolean hasRecentContext = StrUtil.isNotBlank(recentUserContext);
        boolean followUpQuestion = looksLikeFollowUpQuestion(normalizedQuestion, hasRecentContext);

        if (!followUpQuestion || !hasRecentContext) {
            return emptyContext(totalBudget, followUpQuestion);
        }

        String recentPart = renderRecentContext(recentUserContext, totalBudget);
        if (recentPart.isBlank()) {
            return emptyContext(totalBudget, followUpQuestion);
        }
        return AnswerHistoryContext.builder()
            .renderedText(recentPart)
            .structuredContext("")
            .recentContext(recentPart)
            .followUpQuestion(followUpQuestion)
            .totalBudget(totalBudget)
            .recentBudget(totalBudget)
            .structuredBudget(0)
            .build();
    }

    private AnswerHistoryContext emptyContext(int totalBudget, boolean followUpQuestion) {
        return AnswerHistoryContext.builder()
            .renderedText("")
            .structuredContext("")
            .recentContext("")
            .followUpQuestion(followUpQuestion)
            .totalBudget(totalBudget)
            .recentBudget(0)
            .structuredBudget(0)
            .build();
    }

    private String extractRecentUserQuestions(String answerRecentTranscript) {
        String normalized = safeText(answerRecentTranscript);
        if (normalized.startsWith("【最近相关对话】")) {
            normalized = normalized.substring("【最近相关对话】".length()).trim();
        }
        if (normalized.startsWith("最近相关对话：")) {
            normalized = normalized.substring("最近相关对话：".length()).trim();
        }
        StringBuilder builder = new StringBuilder();
        for (String line : normalized.split("\n")) {
            String trimmed = safeText(line);
            if (!trimmed.startsWith("用户：")) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(trimmed);
        }
        return builder.toString().trim();
    }

    private boolean looksLikeFollowUpQuestion(String normalizedQuestion, boolean hasRecentContext) {
        if (!hasRecentContext || StrUtil.isBlank(normalizedQuestion)) {
            return false;
        }
        if (FOLLOW_UP_HINTS.stream().anyMatch(normalizedQuestion::contains)) {
            return true;
        }
        if (normalizedQuestion.matches(".*第\\s*[0-9一二三四五六七八九十百]+\\s*(条|点|项).*")) {
            return true;
        }
        if (normalizedQuestion.length() <= 12) {
            return true;
        }
        return normalizedQuestion.length() <= 18 && (normalizedQuestion.endsWith("呢") || normalizedQuestion.endsWith("吗"));
    }

    private String renderRecentContext(String recentUserContext, int budget) {
        if (budget <= 0 || StrUtil.isBlank(recentUserContext)) {
            return "";
        }
        String title = "对话承接上下文（仅用于理解指代，不作为事实证据）：\n";
        if (budget <= title.length()) {
            return clipTail(recentUserContext, budget);
        }
        String body = clipTail(recentUserContext, budget - title.length());
        if (body.isBlank()) {
            return "";
        }
        return title + body;
    }

    private String clipTail(String text, int maxChars) {
        String normalized = safeText(text);
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        if (maxChars <= 1) {
            return "";
        }
        int start = Math.max(0, normalized.length() - (maxChars - 1));
        return "…" + normalized.substring(start);
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }
}
