package org.javaup.ai.manage.support;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.manage.config.DocumentManageProperties;
import org.javaup.ai.prompt.PromptTemplateNames;
import org.javaup.ai.prompt.PromptTemplateService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 支撑组件
 * @author: 阿星不是程序员
 **/

@Slf4j
@Component
public class DocumentStructureAmbiguityResolver {

    private final DocumentManageProperties properties;
    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ObjectMapper objectMapper;
    private final PromptTemplateService promptTemplateService;

    public DocumentStructureAmbiguityResolver(DocumentManageProperties properties,
                                              ObjectProvider<ChatModel> chatModelProvider,
                                              ObjectMapper objectMapper,
                                              PromptTemplateService promptTemplateService) {
        this.properties = properties;
        this.chatModelProvider = chatModelProvider;
        this.objectMapper = objectMapper;
        this.promptTemplateService = promptTemplateService;
    }

    public List<DocumentStructureSignal> resolve(String documentTitle,
                                                 List<String> allLines,
                                                 List<DocumentStructureSignal> sourceSignals) {
        if (sourceSignals == null || sourceSignals.isEmpty()) {
            return List.of();
        }
        if (!Boolean.TRUE.equals(properties.getStructureParsing().getLlmDisambiguationEnabled())) {
            return sourceSignals;
        }
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return sourceSignals;
        }

        List<DocumentStructureSignal> ambiguousSignals = sourceSignals.stream()
            .filter(signal -> signal != null
                && signal.isAmbiguous()
                && signal.getConfidence() >= properties.getStructureParsing().getAmbiguityConfidenceFloor()
                && signal.getConfidence() <= properties.getStructureParsing().getAmbiguityConfidenceCeil())
            .limit(Math.max(1, properties.getStructureParsing().getMaxAmbiguousSignalsPerCall()))
            .toList();
        if (ambiguousSignals.isEmpty()) {
            return sourceSignals;
        }

        try {
            String prompt = buildPrompt(documentTitle, ambiguousSignals, allLines);
            String content = ChatClient.builder(chatModel)
                .build()
                .prompt()
                .user(prompt)
                .call()
                .content();
            List<DisambiguationResult> results = parse(content);
            if (results.isEmpty()) {
                return sourceSignals;
            }
            Map<Integer, DisambiguationResult> resultMap = new LinkedHashMap<>();
            for (DisambiguationResult result : results) {
                if (result.lineNo == null) {
                    continue;
                }
                resultMap.put(result.lineNo, result);
            }
            List<DocumentStructureSignal> merged = new ArrayList<>(sourceSignals.size());
            for (DocumentStructureSignal signal : sourceSignals) {
                DisambiguationResult resolved = signal == null ? null : resultMap.get(signal.getLineNo());
                merged.add(applyResult(signal, resolved));
            }
            return merged;
        }
        catch (Exception exception) {
            log.warn("结构歧义判定失败，回退到规则结果: {}", exception.getMessage());
            return sourceSignals;
        }
    }

    private String buildPrompt(String documentTitle,
                               List<DocumentStructureSignal> ambiguousSignals,
                               List<String> allLines) {
        return promptTemplateService.render(PromptTemplateNames.DOCUMENT_STRUCTURE_AMBIGUITY, Map.of(
            "documentTitle", StrUtil.blankToDefault(documentTitle, "未命名文档"),
            "candidateBlocks", buildCandidateBlocks(ambiguousSignals, allLines)
        ));
    }

    private String buildCandidateBlocks(List<DocumentStructureSignal> ambiguousSignals,
                                        List<String> allLines) {
        StringBuilder builder = new StringBuilder();
        List<String> safeLines = allLines == null ? List.of() : allLines;
        int contextWindow = Math.max(1, properties.getStructureParsing().getContextWindowLines());
        for (DocumentStructureSignal signal : ambiguousSignals) {
            if (signal == null) {
                continue;
            }
            int currentIndex = Math.max(0, signal.getLineNo() - 1);
            int start = Math.max(0, currentIndex - contextWindow);
            int end = Math.min(safeLines.size() - 1, currentIndex + contextWindow);
            StringBuilder contextBuilder = new StringBuilder();
            for (int index = start; index <= end; index++) {
                contextBuilder.append(index + 1 == signal.getLineNo() ? ">> " : "   ")
                    .append(index + 1)
                    .append(": ")
                    .append(StrUtil.blankToDefault(safeLines.get(index), ""))
                    .append('\n');
            }
            builder.append(promptTemplateService.render(PromptTemplateNames.DOCUMENT_STRUCTURE_AMBIGUITY_CANDIDATE, Map.of(
                "lineNo", signal.getLineNo(),
                "contextLines", contextBuilder.toString().stripTrailing(),
                "initialKind", signal.getKind() == null ? "" : signal.getKind().name(),
                "initialTitle", StrUtil.blankToDefault(signal.getTitle(), ""),
                "initialCode", StrUtil.blankToDefault(signal.getNodeCode(), "")
            ))).append("\n\n");
        }
        return builder.toString().trim();
    }

    private List<DisambiguationResult> parse(String raw) throws Exception {
        if (StrUtil.isBlank(raw)) {
            return List.of();
        }
        String normalized = raw.trim();
        int start = normalized.indexOf('[');
        int end = normalized.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return List.of();
        }
        String jsonArray = normalized.substring(start, end + 1);
        List<Map<String, Object>> items = objectMapper.readValue(jsonArray, new TypeReference<List<Map<String, Object>>>() {
        });
        List<DisambiguationResult> results = new ArrayList<>();
        for (Map<String, Object> item : items) {
            if (item == null) {
                continue;
            }
            Integer lineNo = item.get("line_no") instanceof Number number ? number.intValue() : null;
            String resolvedKind = item.get("resolved_kind") == null ? "" : String.valueOf(item.get("resolved_kind")).trim();
            Integer levelHint = item.get("level_hint") instanceof Number number ? number.intValue() : null;
            results.add(new DisambiguationResult(lineNo, resolvedKind, levelHint));
        }
        return results;
    }

    private DocumentStructureSignal applyResult(DocumentStructureSignal source,
                                                DisambiguationResult resolved) {
        if (source == null || resolved == null || StrUtil.isBlank(resolved.resolvedKind)) {
            return source;
        }
        DocumentStructureSignalKind targetKind = switch (resolved.resolvedKind.trim().toUpperCase()) {
            case "HEADING" -> DocumentStructureSignalKind.HEADING;
            case "LIST_ITEM" -> DocumentStructureSignalKind.LIST_ITEM;
            default -> DocumentStructureSignalKind.BODY;
        };
        source.setKind(targetKind);
        if (targetKind == DocumentStructureSignalKind.HEADING && resolved.levelHint != null && resolved.levelHint > 0) {
            source.setLevelHint(resolved.levelHint);
        }
        source.getReasons().add("llm-disambiguated");
        source.setConfidence(Math.max(source.getConfidence(), 0.88D));
        return source;
    }

    private record DisambiguationResult(
        Integer lineNo,
        String resolvedKind,
        Integer levelHint
    ) {
    }
}
