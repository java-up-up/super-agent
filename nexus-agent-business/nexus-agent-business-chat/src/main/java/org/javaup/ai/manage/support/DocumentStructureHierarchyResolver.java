package org.javaup.ai.manage.support;

import cn.hutool.core.util.StrUtil;
import org.javaup.enums.DocumentStructureNodeTypeEnum;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 支撑组件
 * @author: 阿星不是程序员
 **/

@Component
public class DocumentStructureHierarchyResolver {

    public List<DocumentStructureNodeDraft> resolve(String documentTitle,
                                                    List<DocumentStructureSignal> signals) {
        List<DocumentStructureNodeDraft> drafts = new ArrayList<>();
        DocumentStructureNodeDraft root = new DocumentStructureNodeDraft();
        root.setNodeNo(1);
        root.setLineNo(0);
        root.setNodeType(DocumentStructureNodeTypeEnum.DOCUMENT.getCode());
        root.setParentNodeNo(null);
        root.setDepth(0);
        root.setNodeCode("");
        root.setTitle(StrUtil.blankToDefault(documentTitle, "文档"));
        root.setAnchorText(StrUtil.blankToDefault(documentTitle, "文档"));
        root.setCanonicalPath("/document");
        root.setSectionPath("");
        root.setSourceFamily("document");
        root.setConfidence(1.0D);
        drafts.add(root);

        int nextNodeNo = 2;
        DocumentStructureNodeDraft currentSection = root;
        DocumentStructureNodeDraft currentListItem = null;
        Deque<ListContext> listStack = new ArrayDeque<>();
        Map<Integer, Integer> latestHeadingByDepth = new LinkedHashMap<>();
        Map<String, Integer> latestHeadingByNumericPath = new LinkedHashMap<>();

        for (DocumentStructureSignal signal : signals) {
            if (signal == null || signal.getLineNo() == 0) {
                continue;
            }
            switch (signal.getKind()) {
                case BLANK -> {
                    currentListItem = null;
                    listStack.clear();
                }
                case NOISE -> {
                }
                case TABLE_ROW, QUOTE, BODY -> {
                    appendBody(signal, currentSection, currentListItem, root, drafts);
                }
                case STEP_ITEM, LIST_ITEM -> {
                    DocumentStructureNodeDraft listParent = resolveListParent(signal, currentSection == null ? root : currentSection, listStack, root);
                    DocumentStructureNodeDraft listNode = buildListNode(signal, nextNodeNo++, listParent);
                    drafts.add(listNode);
                    currentListItem = listNode;
                    registerListContext(signal, listNode, listStack);
                    if (currentSection != null) {
                        currentSection.appendLine(signal.getNormalizedText());
                    }
                }
                case HEADING, HEADING_CANDIDATE -> {
                    DocumentStructureNodeDraft headingNode = buildHeadingNode(
                        signal,
                        nextNodeNo++,
                        drafts,
                        latestHeadingByDepth,
                        latestHeadingByNumericPath
                    );
                    drafts.add(headingNode);
                    currentSection = headingNode;
                    currentListItem = null;
                    listStack.clear();
                }
                default -> appendBody(signal, currentSection, currentListItem, root, drafts);
            }
        }

        drafts.sort(Comparator.comparing(DocumentStructureNodeDraft::getNodeNo));
        return drafts;
    }

    private void appendBody(DocumentStructureSignal signal,
                            DocumentStructureNodeDraft currentSection,
                            DocumentStructureNodeDraft currentListItem,
                            DocumentStructureNodeDraft root,
                            List<DocumentStructureNodeDraft> drafts) {
        String line = signal == null ? "" : signal.getNormalizedText();
        if (StrUtil.isBlank(line)) {
            return;
        }
        DocumentStructureNodeDraft target = currentListItem != null ? currentListItem : (currentSection == null ? root : currentSection);
        target.appendLine(line);
        if (currentListItem != null && currentSection != null && !Objects.equals(currentSection.getNodeNo(), currentListItem.getNodeNo())) {
            currentSection.appendLine(line);
        }
        if (currentSection == null && target != root) {
            root.appendLine(line);
        }
    }

    private DocumentStructureNodeDraft buildListNode(DocumentStructureSignal signal,
                                                     int nodeNo,
                                                     DocumentStructureNodeDraft parent) {
        DocumentStructureNodeDraft draft = new DocumentStructureNodeDraft();
        draft.setNodeNo(nodeNo);
        draft.setLineNo(signal.getLineNo());
        draft.setNodeType(signal.getKind() == DocumentStructureSignalKind.STEP_ITEM
            ? DocumentStructureNodeTypeEnum.STEP.getCode()
            : DocumentStructureNodeTypeEnum.LIST_ITEM.getCode());
        draft.setParentNodeNo(parent == null ? 1 : parent.getNodeNo());
        draft.setDepth((parent == null ? 0 : parent.getDepth()) + 1);
        draft.setNodeCode(StrUtil.blankToDefault(signal.getNodeCode(), signal.getItemIndex() == null ? "" : String.valueOf(signal.getItemIndex())));
        draft.setTitle(signal.getTitle());
        draft.setAnchorText(StrUtil.blankToDefault(signal.getNormalizedText(), signal.getTitle()));
        draft.setItemIndex(signal.getItemIndex());
        draft.setSourceFamily(signal.getKind() == DocumentStructureSignalKind.STEP_ITEM ? "step" : "list");
        draft.setConfidence(signal.getConfidence());
        draft.appendLine(signal.getNormalizedText());
        return draft;
    }

    private DocumentStructureNodeDraft resolveListParent(DocumentStructureSignal signal,
                                                         DocumentStructureNodeDraft currentSection,
                                                         Deque<ListContext> listStack,
                                                         DocumentStructureNodeDraft root) {
        int indentLevel = safeIndentLevel(signal);
        while (!listStack.isEmpty() && listStack.peekLast().indentLevel() >= indentLevel) {
            listStack.removeLast();
        }
        if (!listStack.isEmpty() && indentLevel > listStack.peekLast().indentLevel()) {
            return listStack.peekLast().node();
        }
        return currentSection == null ? root : currentSection;
    }

    private void registerListContext(DocumentStructureSignal signal,
                                     DocumentStructureNodeDraft listNode,
                                     Deque<ListContext> listStack) {
        int indentLevel = safeIndentLevel(signal);
        while (!listStack.isEmpty() && listStack.peekLast().indentLevel() >= indentLevel) {
            listStack.removeLast();
        }
        listStack.addLast(new ListContext(listNode, indentLevel));
    }

    private DocumentStructureNodeDraft buildHeadingNode(DocumentStructureSignal signal,
                                                        int nodeNo,
                                                        List<DocumentStructureNodeDraft> drafts,
                                                        Map<Integer, Integer> latestHeadingByDepth,
                                                        Map<String, Integer> latestHeadingByNumericPath) {
        int depth = resolveHeadingDepth(signal, drafts, latestHeadingByDepth, latestHeadingByNumericPath);
        Integer parentNodeNo = resolveHeadingParentNodeNo(signal, depth, drafts, latestHeadingByDepth, latestHeadingByNumericPath);

        DocumentStructureNodeDraft draft = new DocumentStructureNodeDraft();
        draft.setNodeNo(nodeNo);
        draft.setLineNo(signal.getLineNo());
        draft.setNodeType(DocumentStructureNodeTypeEnum.SECTION.getCode());
        draft.setParentNodeNo(parentNodeNo);
        draft.setDepth(depth);
        draft.setNodeCode(StrUtil.blankToDefault(signal.getNodeCode(), ""));
        draft.setTitle(signal.getTitle());
        draft.setAnchorText(buildHeadingAnchorText(signal));
        draft.setNumericPath(signal.getNumericPath() == null ? List.of() : new ArrayList<>(signal.getNumericPath()));
        draft.setSourceFamily(resolveHeadingFamily(signal));
        draft.setConfidence(signal.getConfidence());
        draft.appendLine(signal.getNormalizedText());

        latestHeadingByDepth.entrySet().removeIf(entry -> entry.getKey() >= depth);
        latestHeadingByDepth.put(depth, nodeNo);
        String numericKey = numericKey(draft.getNumericPath());
        if (StrUtil.isNotBlank(numericKey)) {
            latestHeadingByNumericPath.put(numericKey, nodeNo);
        }
        return draft;
    }

    private int resolveHeadingDepth(DocumentStructureSignal signal,
                                    List<DocumentStructureNodeDraft> drafts,
                                    Map<Integer, Integer> latestHeadingByDepth,
                                    Map<String, Integer> latestHeadingByNumericPath) {
        String family = resolveHeadingFamily(signal);
        List<Integer> numericPath = signal.getNumericPath() == null ? List.of() : signal.getNumericPath();
        if ("markdown".equals(family)) {
            return Math.max(1, safeLevel(signal.getLevelHint(), 1));
        }
        if ("chapter".equals(family) || "appendix".equals(family)) {
            return 1;
        }
        if ("decimal".equals(family)) {
            if (numericPath.size() <= 1) {
                return 1;
            }
            Integer parentNodeNo = latestHeadingByNumericPath.get(numericKey(numericPath.subList(0, numericPath.size() - 1)));
            if (parentNodeNo != null) {
                DocumentStructureNodeDraft parent = findByNodeNo(drafts, parentNodeNo);
                if (parent != null) {
                    return parent.getDepth() + 1;
                }
            }
            Integer chapterParent = latestHeadingByNumericPath.get(numericKey(List.of(numericPath.get(0))));
            if (chapterParent != null) {
                DocumentStructureNodeDraft parent = findByNodeNo(drafts, chapterParent);
                if (parent != null) {
                    return parent.getDepth() + 1;
                }
            }
            return numericPath.size();
        }
        return Math.max(1, safeLevel(signal.getLevelHint(), 1));
    }

    private Integer resolveHeadingParentNodeNo(DocumentStructureSignal signal,
                                               int depth,
                                               List<DocumentStructureNodeDraft> drafts,
                                               Map<Integer, Integer> latestHeadingByDepth,
                                               Map<String, Integer> latestHeadingByNumericPath) {
        String family = resolveHeadingFamily(signal);
        List<Integer> numericPath = signal.getNumericPath() == null ? List.of() : signal.getNumericPath();
        if ("chapter".equals(family) || "appendix".equals(family)) {
            return 1;
        }
        if ("decimal".equals(family) && numericPath.size() > 1) {
            Integer exactParent = latestHeadingByNumericPath.get(numericKey(numericPath.subList(0, numericPath.size() - 1)));
            if (exactParent != null) {
                return exactParent;
            }
            Integer chapterParent = latestHeadingByNumericPath.get(numericKey(List.of(numericPath.get(0))));
            if (chapterParent != null) {
                return chapterParent;
            }
        }
        return findNearestParentByDepth(depth, latestHeadingByDepth);
    }

    private Integer findNearestParentByDepth(int depth,
                                             Map<Integer, Integer> latestHeadingByDepth) {
        for (int candidateDepth = depth - 1; candidateDepth >= 1; candidateDepth--) {
            Integer parentNodeNo = latestHeadingByDepth.get(candidateDepth);
            if (parentNodeNo != null) {
                return parentNodeNo;
            }
        }
        return 1;
    }

    private String resolveHeadingFamily(DocumentStructureSignal signal) {
        if (signal == null || signal.getReasons() == null) {
            return "plain";
        }
        if (signal.getReasons().contains("markdown-heading")) {
            return "markdown";
        }
        if (signal.getReasons().contains("chapter-heading")) {
            return "chapter";
        }
        if (signal.getReasons().contains("appendix-heading")) {
            return "appendix";
        }
        if (signal.getReasons().contains("decimal-heading")) {
            return "decimal";
        }
        if (signal.getReasons().contains("single-digit-ambiguous-heading")) {
            return "decimal";
        }
        return "plain";
    }

    private String buildHeadingAnchorText(DocumentStructureSignal signal) {
        String code = StrUtil.blankToDefault(signal.getNodeCode(), "").trim();
        String title = StrUtil.blankToDefault(signal.getTitle(), "").trim();
        if (StrUtil.isBlank(code)) {
            return title;
        }
        if (title.startsWith(code)) {
            return title;
        }
        return code + " " + title;
    }

    private String numericKey(List<Integer> numericPath) {
        if (numericPath == null || numericPath.isEmpty()) {
            return "";
        }
        return numericPath.stream().map(String::valueOf).reduce((left, right) -> left + "." + right).orElse("");
    }

    private int safeLevel(Integer levelHint, int defaultValue) {
        return levelHint == null || levelHint <= 0 ? defaultValue : levelHint;
    }

    private int safeIndentLevel(DocumentStructureSignal signal) {
        if (signal == null || signal.getIndentLevel() == null || signal.getIndentLevel() < 0) {
            return 0;
        }
        return signal.getIndentLevel();
    }

    private DocumentStructureNodeDraft findByNodeNo(List<DocumentStructureNodeDraft> drafts, Integer nodeNo) {
        if (nodeNo == null) {
            return null;
        }
        for (DocumentStructureNodeDraft draft : drafts) {
            if (draft != null && nodeNo.equals(draft.getNodeNo())) {
                return draft;
            }
        }
        return null;
    }

    private record ListContext(
        DocumentStructureNodeDraft node,
        int indentLevel
    ) {
    }
}
