package org.javaup.ai.manage.support;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import org.javaup.enums.DocumentStructureNodeTypeEnum;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 支撑组件
 * @author: 阿星不是程序员
 **/
@AllArgsConstructor
@Component
public class DocumentStructureNodeExtractor {

    private final DocumentStructureSignalExtractor signalExtractor;
    private final DocumentStructureAmbiguityResolver ambiguityResolver;
    private final DocumentStructureHierarchyResolver hierarchyResolver;
    private final DocumentStructureTreeValidator treeValidator;

    public List<DocumentStructureNodeCandidate> extract(String documentTitle, String parsedText) {
        String normalizedTitle = StrUtil.blankToDefault(documentTitle, "文档").trim();
        String normalizedText = StrUtil.blankToDefault(parsedText, "").trim();
        if (normalizedText.isBlank()) {
            return List.of(new DocumentStructureNodeCandidate(
                1,
                DocumentStructureNodeTypeEnum.DOCUMENT.getCode(),
                null,
                0,
                0,
                0,
                "",
                normalizedTitle,
                normalizedTitle,
                "/document",
                "",
                "",
                null
            ));
        }

        DocumentStructureSignalBatch signalBatch = signalExtractor.extract(normalizedTitle, normalizedText);
        List<DocumentStructureSignal> rawSignals = signalBatch == null ? List.of() : signalBatch.signals();
        List<String> allLines = signalBatch == null ? List.of() : signalBatch.contextLines();
        List<DocumentStructureSignal> resolvedSignals = ambiguityResolver.resolve(normalizedTitle, allLines, rawSignals);
        List<DocumentStructureNodeDraft> drafts = hierarchyResolver.resolve(normalizedTitle, resolvedSignals);
        return treeValidator.validateAndBuild(normalizedTitle, drafts);
    }
}
