package org.javaup.ai.chatagent.rag.service;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.chatagent.rag.model.ConversationExecutionPlan;
import org.javaup.ai.chatagent.rag.model.DocumentNavigationDecision;
import org.javaup.ai.chatagent.rag.model.HistoryPlanningContext;
import org.javaup.ai.manage.model.DocumentRetrieveFilters;
import org.javaup.ai.manage.model.DocumentRetrieveRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/

@Slf4j
@Component
public class DocumentRetrieveRequestFactory {

    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(20\\d{2})\\b");
    private static final Pattern SECTION_PATTERN = Pattern.compile("(第\\s*[一二三四五六七八九十百0-9]+\\s*[章节条部分])|(附录\\s*[A-Za-z一二三四五六七八九十0-9]+)");

    private static final List<String> DOCUMENT_NAME_HINTS = List.of(
        "部署手册", "配置手册", "操作手册", "用户手册", "快速开始", "接入指南", "FAQ", "常见问题",
        "说明文档", "说明书", "规范", "指南", "手册", "文档"
    );

    private static final List<String> BUSINESS_CATEGORY_HINTS = List.of(
        "流程", "规则", "操作手册", "部署", "配置", "接入", "协议", "故障", "排错", "规范", "说明"
    );

    private static final List<String> DOCUMENT_TAG_HINTS = List.of(
        "2024", "2025", "2026", "部署", "配置", "接入", "协议", "FAQ", "故障", "排错", "升级", "兼容"
    );

    public DocumentRetrieveRequest build(String subQuestion, ConversationExecutionPlan plan, int topK) {
        String normalizedQuestion = StrUtil.blankToDefault(subQuestion, "").trim();
        QueryAugmentation augmentation = buildQueryAugmentation(
            normalizedQuestion,
            plan.getHistoryPlanningContext(),
            plan.getNavigationDecision()
        );
        DocumentRetrieveFilters filters = buildFilters(normalizedQuestion);
        DocumentRetrieveRequest request = new DocumentRetrieveRequest(
            normalizedQuestion,
            augmentation.retrievalQuery(),
            plan.getSelectedDocumentId(),
            plan.getSelectedTaskId(),
            topK,
            filters,
            augmentation.queryContextHints()
        );
        request.setDocumentIds(plan.getRetrievalDocumentIds() == null || plan.getRetrievalDocumentIds().isEmpty()
            ? (plan.getSelectedDocumentId() == null ? List.of() : List.of(plan.getSelectedDocumentId()))
            : plan.getRetrievalDocumentIds());
        request.setTaskIds(plan.getRetrievalTaskIds() == null || plan.getRetrievalTaskIds().isEmpty()
            ? (plan.getSelectedTaskId() == null ? List.of() : List.of(plan.getSelectedTaskId()))
            : plan.getRetrievalTaskIds());
        log.info("检索请求构造: originalSubQuestion='{}', retrievalQuery='{}', documentId={}, taskId={}, sectionHints={}, yearHints={}, queryContextHints={}",
            normalizedQuestion,
            request.getRetrievalQuery(),
            request.getDocumentId(),
            request.getTaskId(),
            filters == null ? List.of() : filters.getSectionPathHints(),
            filters == null ? List.of() : filters.getYearHints(),
            request.getQueryContextHints());
        return request;
    }

    private QueryAugmentation buildQueryAugmentation(String normalizedQuestion,
                                                     HistoryPlanningContext historyPlanningContext,
                                                     DocumentNavigationDecision navigationDecision) {
        if (StrUtil.isBlank(normalizedQuestion)) {
            return new QueryAugmentation("", List.of());
        }
        List<String> navigationHints = navigationDecision == null || navigationDecision.getQueryContextHints() == null
            ? List.of()
            : navigationDecision.getQueryContextHints().stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .limit(4)
                .toList();
        if (!looksLikeShortFollowUp(normalizedQuestion)
            || historyPlanningContext == null
            || historyPlanningContext.getQueryContextHints() == null
            || historyPlanningContext.getQueryContextHints().isEmpty()) {
            if (navigationHints.isEmpty()) {
                return new QueryAugmentation(normalizedQuestion, extractMeaningfulTerms(normalizedQuestion));
            }
            String retrievalQuery = (normalizedQuestion + " " + String.join(" ", navigationHints)).trim();
            List<String> queryHints = new ArrayList<>(navigationHints);
            queryHints.addAll(extractMeaningfulTerms(normalizedQuestion));
            return new QueryAugmentation(retrievalQuery, queryHints.stream().distinct().limit(8).toList());
        }
        List<String> normalizedHints = historyPlanningContext.getQueryContextHints().stream()
            .filter(StrUtil::isNotBlank)
            .map(String::trim)
            .distinct()
            .limit(4)
            .toList();
        List<String> allHints = new ArrayList<>(normalizedHints);
        allHints.addAll(navigationHints);
        if (allHints.isEmpty()) {
            return new QueryAugmentation(normalizedQuestion, extractMeaningfulTerms(normalizedQuestion));
        }
        String retrievalQuery = (normalizedQuestion + " " + String.join(" ", allHints)).trim();
        List<String> queryContextHints = new ArrayList<>(allHints);
        queryContextHints.addAll(extractMeaningfulTerms(normalizedQuestion));
        return new QueryAugmentation(retrievalQuery, queryContextHints.stream().distinct().limit(8).toList());
    }

    private DocumentRetrieveFilters buildFilters(String question) {
        if (StrUtil.isBlank(question)) {
            return DocumentRetrieveFilters.builder().build();
        }
        String normalized = question.toLowerCase(Locale.ROOT);
        LinkedHashSet<String> documentNameHints = new LinkedHashSet<>();
        LinkedHashSet<String> businessCategoryHints = new LinkedHashSet<>();
        LinkedHashSet<String> documentTagHints = new LinkedHashSet<>();
        LinkedHashSet<String> sectionPathHints = new LinkedHashSet<>();
        LinkedHashSet<String> yearHints = new LinkedHashSet<>();

        Matcher yearMatcher = YEAR_PATTERN.matcher(question);
        while (yearMatcher.find()) {
            yearHints.add(yearMatcher.group(1));
        }

        Matcher sectionMatcher = SECTION_PATTERN.matcher(question);
        while (sectionMatcher.find()) {
            if (StrUtil.isNotBlank(sectionMatcher.group())) {
                sectionPathHints.add(sectionMatcher.group().replaceAll("\\s+", ""));
            }
        }

        for (String hint : DOCUMENT_NAME_HINTS) {
            if (normalized.contains(hint.toLowerCase(Locale.ROOT))) {
                documentNameHints.add(hint);
            }
        }
        for (String hint : BUSINESS_CATEGORY_HINTS) {
            if (normalized.contains(hint.toLowerCase(Locale.ROOT))) {
                businessCategoryHints.add(hint);
            }
        }
        for (String hint : DOCUMENT_TAG_HINTS) {
            if (normalized.contains(hint.toLowerCase(Locale.ROOT))) {
                documentTagHints.add(hint);
            }
        }

        return DocumentRetrieveFilters.builder()
            .documentNameHints(new ArrayList<>(documentNameHints))
            .businessCategoryHints(new ArrayList<>(businessCategoryHints))
            .documentTagHints(new ArrayList<>(documentTagHints))
            .sectionPathHints(new ArrayList<>(sectionPathHints))
            .yearHints(new ArrayList<>(yearHints))
            .build();
    }

    private boolean looksLikeShortFollowUp(String question) {
        if (StrUtil.isBlank(question)) {
            return false;
        }
        return question.length() < 12
            || question.contains("它")
            || question.contains("这个")
            || question.contains("那个")
            || question.contains("刚才")
            || question.contains("前面")
            || question.contains("上面");
    }

    private List<String> extractMeaningfulTerms(String question) {
        if (StrUtil.isBlank(question)) {
            return List.of();
        }
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        for (String segment : question.split("[\\s、，,；;：:（）()\\-的和及与或]+")) {
            String trimmed = segment.trim();
            if (trimmed.length() >= 2) {
                terms.add(trimmed);
            }
        }
        return new ArrayList<>(terms).stream().limit(6).toList();
    }

    private record QueryAugmentation(String retrievalQuery, List<String> queryContextHints) {
    }
}
