package org.javaup.ai.manage.support;

import cn.hutool.core.util.StrUtil;
import org.javaup.enums.DocumentStructureNodeTypeEnum;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
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
public class DocumentStructureTreeValidator {

    public List<DocumentStructureNodeCandidate> validateAndBuild(String documentTitle,
                                                                 List<DocumentStructureNodeDraft> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            return List.of();
        }
        Map<Integer, DocumentStructureNodeDraft> draftMap = new LinkedHashMap<>();
        for (DocumentStructureNodeDraft draft : drafts) {
            if (draft != null && draft.getNodeNo() != null) {
                draftMap.put(draft.getNodeNo(), draft);
            }
        }

        collapseSyntheticTitleSection(documentTitle, draftMap);
        repairNumberedHierarchy(draftMap);
        repairInvalidParents(draftMap);
        recomputeDepths(draftMap);
        rebuildPaths(documentTitle, draftMap);
        rebuildSiblingLinks(draftMap);

        return draftMap.values().stream()
            .sorted(Comparator.comparingInt(DocumentStructureNodeDraft::getNodeNo))
            .map(this::toCandidate)
            .toList();
    }

    private void collapseSyntheticTitleSection(String documentTitle,
                                               Map<Integer, DocumentStructureNodeDraft> draftMap) {
        String normalizedTitle = normalizeComparableTitle(documentTitle);
        if (normalizedTitle.isBlank()) {
            return;
        }
        Integer duplicateNodeNo = null;
        for (DocumentStructureNodeDraft draft : draftMap.values()) {
            if (draft == null
                || draft.getNodeNo() == null
                || draft.getNodeNo() == 1
                || !draft.isSection()
                || !Objects.equals(draft.getParentNodeNo(), 1)
                || StrUtil.isNotBlank(draft.getNodeCode())) {
                continue;
            }
            if (normalizedTitle.equals(normalizeComparableTitle(draft.getTitle()))) {
                duplicateNodeNo = draft.getNodeNo();
                break;
            }
        }
        if (duplicateNodeNo == null) {
            return;
        }
        for (DocumentStructureNodeDraft draft : draftMap.values()) {
            if (draft != null && Objects.equals(draft.getParentNodeNo(), duplicateNodeNo)) {
                draft.setParentNodeNo(1);
            }
        }
        draftMap.remove(duplicateNodeNo);
    }

    private void repairNumberedHierarchy(Map<Integer, DocumentStructureNodeDraft> draftMap) {
        Map<String, Integer> numericPathMap = new LinkedHashMap<>();
        for (DocumentStructureNodeDraft draft : draftMap.values()) {
            if (draft == null || !draft.isSection()) {
                continue;
            }
            String key = numericKey(draft.getNumericPath());
            if (StrUtil.isNotBlank(key)) {
                numericPathMap.putIfAbsent(key, draft.getNodeNo());
            }
        }

        for (DocumentStructureNodeDraft draft : draftMap.values()) {
            if (draft == null || !draft.isSection()) {
                continue;
            }
            List<Integer> numericPath = draft.getNumericPath();
            if (numericPath == null || numericPath.isEmpty()) {
                continue;
            }
            if (numericPath.size() == 1) {
                draft.setParentNodeNo(1);
                continue;
            }
            String directParentKey = numericKey(numericPath.subList(0, numericPath.size() - 1));
            Integer directParent = numericPathMap.get(directParentKey);
            if (directParent != null) {
                draft.setParentNodeNo(directParent);
                continue;
            }
            String chapterParentKey = numericKey(List.of(numericPath.get(0)));
            Integer chapterParent = numericPathMap.get(chapterParentKey);
            if (chapterParent != null) {
                draft.setParentNodeNo(chapterParent);
            }
        }
    }

    private void repairInvalidParents(Map<Integer, DocumentStructureNodeDraft> draftMap) {
        for (DocumentStructureNodeDraft draft : draftMap.values()) {
            if (draft == null || draft.getNodeNo() == 1) {
                continue;
            }
            DocumentStructureNodeDraft parent = draft.getParentNodeNo() == null ? null : draftMap.get(draft.getParentNodeNo());
            if (parent == null) {
                draft.setParentNodeNo(1);
                continue;
            }
            if (draft.isSection() && parent.isListLike()) {
                draft.setParentNodeNo(parent.getParentNodeNo() == null ? 1 : parent.getParentNodeNo());
            }
        }
    }

    private void recomputeDepths(Map<Integer, DocumentStructureNodeDraft> draftMap) {
        DocumentStructureNodeDraft root = draftMap.get(1);
        if (root == null) {
            return;
        }
        root.setDepth(0);
        List<DocumentStructureNodeDraft> ordered = draftMap.values().stream()
            .sorted(Comparator.comparingInt(DocumentStructureNodeDraft::getNodeNo))
            .toList();
        for (DocumentStructureNodeDraft draft : ordered) {
            if (draft == null || draft.getNodeNo() == 1) {
                continue;
            }
            DocumentStructureNodeDraft parent = draftMap.get(draft.getParentNodeNo());
            draft.setDepth(parent == null ? 1 : parent.getDepth() + 1);
        }
    }

    private void rebuildPaths(String documentTitle,
                              Map<Integer, DocumentStructureNodeDraft> draftMap) {
        for (DocumentStructureNodeDraft draft : draftMap.values()) {
            if (draft == null) {
                continue;
            }
            if (draft.getNodeNo() == 1) {
                draft.setCanonicalPath("/document");
                draft.setSectionPath("");
                continue;
            }
            DocumentStructureNodeDraft parent = draftMap.get(draft.getParentNodeNo());
            String parentCanonicalPath = parent == null ? "/document" : StrUtil.blankToDefault(parent.getCanonicalPath(), "/document");
            String parentSectionPath = parent == null ? "" : StrUtil.blankToDefault(parent.getSectionPath(), "");
            String segment = buildPathSegment(draft);
            draft.setCanonicalPath(parentCanonicalPath + "/" + segment);
            if (draft.isSection()) {
                draft.setSectionPath(joinSectionPath(parentSectionPath, displayTitle(draft)));
            }
            else {
                draft.setSectionPath(parentSectionPath);
            }
        }
    }

    private void rebuildSiblingLinks(Map<Integer, DocumentStructureNodeDraft> draftMap) {
        Map<Integer, List<DocumentStructureNodeDraft>> childrenByParent = new LinkedHashMap<>();
        for (DocumentStructureNodeDraft draft : draftMap.values()) {
            if (draft == null || draft.getNodeNo() == 1) {
                continue;
            }
            childrenByParent.computeIfAbsent(draft.getParentNodeNo(), ignored -> new ArrayList<>()).add(draft);
        }
        for (List<DocumentStructureNodeDraft> siblings : childrenByParent.values()) {
            siblings.sort(Comparator.comparingInt(DocumentStructureNodeDraft::getLineNo));
            for (int index = 0; index < siblings.size(); index++) {
                DocumentStructureNodeDraft current = siblings.get(index);
                current.setPrevSiblingNodeNo(index == 0 ? 0 : siblings.get(index - 1).getNodeNo());
                current.setNextSiblingNodeNo(index == siblings.size() - 1 ? 0 : siblings.get(index + 1).getNodeNo());
            }
        }
    }

    private DocumentStructureNodeCandidate toCandidate(DocumentStructureNodeDraft draft) {
        return new DocumentStructureNodeCandidate(
            draft.getNodeNo(),
            draft.getNodeType(),
            draft.getParentNodeNo(),
            normalizeSibling(draft.getPrevSiblingNodeNo()),
            normalizeSibling(draft.getNextSiblingNodeNo()),
            draft.getDepth(),
            draft.getNodeCode(),
            draft.getTitle(),
            draft.getAnchorText(),
            draft.getCanonicalPath(),
            draft.getSectionPath(),
            draft.contentText(),
            draft.getItemIndex()
        );
    }

    private Integer normalizeSibling(Integer siblingNodeNo) {
        return siblingNodeNo == null ? 0 : siblingNodeNo;
    }

    private String joinSectionPath(String parentSectionPath, String currentTitle) {
        if (StrUtil.isBlank(parentSectionPath)) {
            return StrUtil.blankToDefault(currentTitle, "");
        }
        if (StrUtil.isBlank(currentTitle)) {
            return parentSectionPath;
        }
        return parentSectionPath + " > " + currentTitle;
    }

    private String buildPathSegment(DocumentStructureNodeDraft draft) {
        if (draft == null) {
            return "node";
        }
        if (draft.isListLike()) {
            if (draft.getItemIndex() != null && draft.getItemIndex() > 0) {
                return "item-" + draft.getItemIndex();
            }
            return slug(displayTitle(draft));
        }
        String code = StrUtil.blankToDefault(draft.getNodeCode(), "").trim();
        if (StrUtil.isNotBlank(code)) {
            return slug(code);
        }
        return slug(displayTitle(draft));
    }

    private String displayTitle(DocumentStructureNodeDraft draft) {
        String code = StrUtil.blankToDefault(draft.getNodeCode(), "").trim();
        String title = StrUtil.blankToDefault(draft.getTitle(), "").trim();
        if (StrUtil.isBlank(code)) {
            return title;
        }
        if (title.startsWith(code)) {
            return title;
        }
        return code + " " + title;
    }

    private String slug(String value) {
        String normalized = StrUtil.blankToDefault(value, "").trim();
        if (normalized.isBlank()) {
            return "node";
        }
        String slug = normalized
            .replaceAll("\\s+", "-")
            .replaceAll("[^\\p{IsHan}A-Za-z0-9_.-]", "");
        return slug.isBlank() ? "node" : slug;
    }

    private String numericKey(List<Integer> numericPath) {
        if (numericPath == null || numericPath.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < numericPath.size(); index++) {
            if (index > 0) {
                builder.append('.');
            }
            Integer segment = numericPath.get(index);
            if (segment != null) {
                builder.append(segment);
            }
        }
        return builder.toString();
    }

    private String normalizeComparableTitle(String text) {
        String normalized = StrUtil.blankToDefault(text, "").trim();
        if (normalized.isBlank()) {
            return "";
        }
        return normalized
            .replaceAll("^#+\\s*", "")
            .replaceAll("\\.[A-Za-z0-9]{1,6}$", "")
            .replaceAll("\\s+", "")
            .toLowerCase();
    }
}
