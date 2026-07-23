package org.javaup.ai.manage.support;

import cn.hutool.core.util.StrUtil;
import org.javaup.ai.manage.config.DocumentManageProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 支撑组件
 * @author: 阿星不是程序员
 **/

@Component
public class DocumentStructureSignalExtractor {

    private static final Pattern MARKDOWN_HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern DECIMAL_HEADING_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)+)\\s*[、.]?\\s*(.+)$");
    private static final Pattern SINGLE_LEVEL_DIGIT_PATTERN = Pattern.compile("^(\\d+)\\s*[、.]\\s*(.+)$");
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("^(第([一二三四五六七八九十百\\d]+)[章节条部分])\\s*(.+)$");
    private static final Pattern APPENDIX_PATTERN = Pattern.compile("^(附录\\s*([A-Za-z一二三四五六七八九十百\\d]+))(?:\\s+(.+))?$");
    private static final Pattern CHINESE_OUTLINE_PATTERN = Pattern.compile("^([一二三四五六七八九十百]+)[、.]\\s*(.+)$");
    private static final Pattern EXPLICIT_STEP_PATTERN = Pattern.compile("^(?:第\\s*([0-9一二三四五六七八九十百]+)\\s*步|步骤\\s*([0-9一二三四五六七八九十百]+))\\s*[:：、.]?\\s*(.+)$");
    private static final Pattern BULLET_PATTERN = Pattern.compile("^([-*+•])\\s+(.+)$");
    private static final Pattern CHECKBOX_PATTERN = Pattern.compile("^\\[(?: |x|X)]\\s+(.+)$");
    private static final Pattern PAGE_NOISE_PATTERN = Pattern.compile("^(?:第\\s*\\d+\\s*页|Page\\s*\\d+|\\d+\\s*/\\s*\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern COPYRIGHT_NOISE_PATTERN = Pattern.compile(".*(?:版权所有|未经授权|内部使用|copyright|all rights reserved|保密).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION_FOOTER_PATTERN = Pattern.compile(".*(?:\\bV\\d+(?:\\.\\d+)*\\b|版本|修订|Rev\\.?\\s*\\d+).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern INLINE_EXPLICIT_STEP_BOUNDARY_PATTERN = Pattern.compile("(?=(?:第\\s*[0-9一二三四五六七八九十百]+\\s*步|步骤\\s*[0-9一二三四五六七八九十百]+)\\s*[:：、.])");
    private static final Pattern TABLE_SPLIT_PATTERN = Pattern.compile("\\|");

    private final DocumentManageProperties properties;
    private final DocumentLineClassifier documentLineClassifier;

    public DocumentStructureSignalExtractor(DocumentManageProperties properties,
                                            DocumentLineClassifier documentLineClassifier) {
        this.properties = properties;
        this.documentLineClassifier = documentLineClassifier;
    }

    public DocumentStructureSignalBatch extract(String documentTitle, String parsedText) {
        String normalizedTitle = safeText(documentTitle);
        List<DocumentStructureLogicalLine> logicalLines = buildLogicalLines(parsedText);
        Map<String, Integer> lineFrequency = buildLineFrequency(logicalLines);
        List<DocumentStructureSignal> signals = new ArrayList<>(logicalLines.size() + 1);
        if (StrUtil.isNotBlank(normalizedTitle)) {
            signals.add(DocumentStructureSignal.builder()
                .lineNo(0)
                .rawText(normalizedTitle)
                .normalizedText(normalizedTitle)
                .kind(DocumentStructureSignalKind.DOCUMENT_TITLE)
                .title(normalizedTitle)
                .levelHint(0)
                .confidence(1.0D)
                .build());
        }

        for (int index = 0; index < logicalLines.size(); index++) {
            DocumentStructureLogicalLine logicalLine = logicalLines.get(index);
            LineContext context = buildContext(logicalLines, index);
            signals.add(classify(normalizedTitle, logicalLine, context, lineFrequency));
        }
        List<String> contextLines = logicalLines.stream()
            .map(DocumentStructureLogicalLine::normalizedText)
            .toList();
        return new DocumentStructureSignalBatch(contextLines, signals);
    }

    private DocumentStructureSignal classify(String documentTitle,
                                             DocumentStructureLogicalLine logicalLine,
                                             LineContext context,
                                             Map<String, Integer> lineFrequency) {
        int lineNo = logicalLine.lineNo();
        String rawText = logicalLine.rawText();
        String normalized = logicalLine.normalizedText();
        String previousNonBlank = context.previousNonBlank() == null ? "" : context.previousNonBlank().normalizedText();
        String nextNonBlank = context.nextNonBlank() == null ? "" : context.nextNonBlank().normalizedText();
        if (normalized.isBlank()) {
            return signal(lineNo, rawText, normalized, logicalLine.indentLevel(), DocumentStructureSignalKind.BLANK, "", "", 0, null, List.of(), 1.0D);
        }
        if (isRepeatedNoise(documentTitle, normalized, lineFrequency.getOrDefault(normalized, 0))) {
            return signal(lineNo, rawText, normalized, logicalLine.indentLevel(), DocumentStructureSignalKind.NOISE, "", "", 0, null,
                List.of("repeated-running-header-or-footer"), 0.99D);
        }
        if (PAGE_NOISE_PATTERN.matcher(normalized).matches()) {
            return signal(lineNo, rawText, normalized, logicalLine.indentLevel(), DocumentStructureSignalKind.NOISE, "", "", 0, null,
                List.of("page-noise"), 0.98D);
        }
        Matcher markdown = MARKDOWN_HEADING_PATTERN.matcher(normalized);
        if (markdown.matches()) {
            String title = markdown.group(2).trim();
            if (sameDocumentTitle(documentTitle, title)) {
                return signal(lineNo, rawText, normalized, logicalLine.indentLevel(), DocumentStructureSignalKind.NOISE, "", title, 0, null,
                    List.of("duplicate-document-title"), 0.99D);
            }
            DocumentStructureSignal signal = signal(lineNo, rawText, normalized, logicalLine.indentLevel(), DocumentStructureSignalKind.HEADING,
                extractCode(title), title, markdown.group(1).length(), null, List.of("markdown-heading"), 0.98D);
            signal.setNumericPath(extractNumericPath(signal.getNodeCode()));
            return signal;
        }
        Matcher explicitStep = EXPLICIT_STEP_PATTERN.matcher(normalized);
        if (explicitStep.matches()) {
            Integer itemIndex = parseLooseNumber(StrUtil.blankToDefault(explicitStep.group(1), explicitStep.group(2)));
            return signal(lineNo, rawText, normalized, logicalLine.indentLevel(), DocumentStructureSignalKind.STEP_ITEM, "", explicitStep.group(3).trim(), null, itemIndex,
                List.of("explicit-step"), 0.96D);
        }
        Matcher chapter = CHAPTER_PATTERN.matcher(normalized);
        if (chapter.matches()) {
            String code = chapter.group(1).trim();
            String title = chapter.group(3).trim();
            if (sameDocumentTitle(documentTitle, title)) {
                return signal(lineNo, rawText, normalized, logicalLine.indentLevel(), DocumentStructureSignalKind.NOISE, code, title, 0, null,
                    List.of("duplicate-document-title"), 0.99D);
            }
            DocumentStructureSignal signal = signal(lineNo, rawText, normalized, logicalLine.indentLevel(), DocumentStructureSignalKind.HEADING,
                code, title, 1, null, List.of("chapter-heading"), 0.96D);
            Integer chapterNo = parseLooseNumber(chapter.group(2));
            if (chapterNo != null && chapterNo > 0) {
                signal.setNumericPath(List.of(chapterNo));
            }
            return signal;
        }
        Matcher appendix = APPENDIX_PATTERN.matcher(normalized);
        if (appendix.matches()) {
            String code = appendix.group(1).trim();
            String title = StrUtil.blankToDefault(appendix.group(3), code).trim();
            return signal(lineNo, rawText, normalized, logicalLine.indentLevel(), DocumentStructureSignalKind.HEADING,
                code, title, 1, null, List.of("appendix-heading"), 0.92D);
        }
        Matcher decimal = DECIMAL_HEADING_PATTERN.matcher(normalized);
        if (decimal.matches()) {
            String code = decimal.group(1).trim();
            String title = decimal.group(2).trim();
            DocumentStructureSignal signal = signal(lineNo, rawText, normalized, logicalLine.indentLevel(), DocumentStructureSignalKind.HEADING,
                code, title, Math.max(1, code.split("\\.").length), null, List.of("decimal-heading"), 0.95D);
            signal.setNumericPath(extractNumericPath(code));
            return signal;
        }
        if (isTableRow(normalized)) {
            return signal(lineNo, rawText, normalized, logicalLine.indentLevel(), DocumentStructureSignalKind.TABLE_ROW, "", normalized, null, null,
                List.of("table-row"), 0.90D);
        }
        if (normalized.startsWith(">")) {
            return signal(lineNo, rawText, normalized, logicalLine.indentLevel(), DocumentStructureSignalKind.QUOTE, "", normalized, null, null,
                List.of("quote"), 0.88D);
        }
        Matcher checkbox = CHECKBOX_PATTERN.matcher(normalized);
        if (checkbox.matches()) {
            return signal(lineNo, rawText, normalized, logicalLine.indentLevel(), DocumentStructureSignalKind.LIST_ITEM, "", checkbox.group(1).trim(), null, null,
                List.of("checkbox-list"), 0.92D);
        }
        Matcher bullet = BULLET_PATTERN.matcher(normalized);
        if (bullet.matches()) {
            return signal(lineNo, rawText, normalized, logicalLine.indentLevel(), DocumentStructureSignalKind.LIST_ITEM, "", bullet.group(2).trim(), null, null,
                List.of("bullet-list"), 0.90D);
        }
        Matcher singleDigit = SINGLE_LEVEL_DIGIT_PATTERN.matcher(normalized);
        if (singleDigit.matches()) {
            String title = singleDigit.group(2).trim();
            Integer itemIndex = parseLooseNumber(singleDigit.group(1));
            boolean sequential = isNeighborSequence(itemIndex, OrderedMarkerFamily.ARABIC_SINGLE, context);
            boolean introducedByLeadIn = previousIntroducesList(context.previousNonBlank());
            boolean headingLike = !sequential
                && !introducedByLeadIn
                && looksLikePlainHeading(title, context);
            DocumentStructureSignal signal = signal(
                lineNo,
                rawText,
                normalized,
                logicalLine.indentLevel(),
                headingLike ? DocumentStructureSignalKind.HEADING_CANDIDATE : DocumentStructureSignalKind.LIST_ITEM,
                singleDigit.group(1).trim(),
                title,
                headingLike ? 1 : null,
                itemIndex,
                List.of(headingLike ? "single-digit-ambiguous-heading"
                    : sequential ? "single-digit-sequence-list" : "single-digit-list"),
                headingLike ? 0.62D : sequential || introducedByLeadIn ? 0.93D : 0.88D
            );
            if (headingLike && itemIndex != null && itemIndex > 0) {
                signal.setNumericPath(List.of(itemIndex));
            }
            return signal;
        }
        Matcher chineseOutline = CHINESE_OUTLINE_PATTERN.matcher(normalized);
        if (chineseOutline.matches()) {
            String title = chineseOutline.group(2).trim();
            Integer index = parseLooseNumber(chineseOutline.group(1));
            boolean sequential = isNeighborSequence(index, OrderedMarkerFamily.CHINESE_OUTLINE, context);
            boolean introducedByLeadIn = previousIntroducesList(context.previousNonBlank());
            boolean headingLike = !sequential
                && !introducedByLeadIn
                && looksLikePlainHeading(title, context);
            DocumentStructureSignal signal = signal(
                lineNo,
                rawText,
                normalized,
                logicalLine.indentLevel(),
                headingLike ? DocumentStructureSignalKind.HEADING_CANDIDATE : DocumentStructureSignalKind.LIST_ITEM,
                chineseOutline.group(1).trim(),
                title,
                headingLike ? 1 : null,
                index,
                List.of(headingLike ? "chinese-outline-ambiguous-heading"
                    : sequential ? "chinese-outline-sequence-list" : "chinese-outline-list"),
                headingLike ? 0.60D : sequential || introducedByLeadIn ? 0.92D : 0.86D
            );
            if (headingLike && index != null && index > 0) {
                signal.setNumericPath(List.of(index));
            }
            return signal;
        }

        DocumentLineClassifier.LineClassification fallback = documentLineClassifier.classify(normalized);
        if (!fallback.isHeading() && looksLikePlainHeading(normalized, context)) {
            return signal(lineNo, rawText, normalized, logicalLine.indentLevel(), DocumentStructureSignalKind.HEADING_CANDIDATE,
                "", normalized, inferPlainHeadingLevel(context), null, List.of("plain-heading-candidate"), 0.58D);
        }
        return signal(lineNo, rawText, normalized, logicalLine.indentLevel(), DocumentStructureSignalKind.BODY,
            "", normalized, null, null, List.of("body"), 1.0D);
    }

    private DocumentStructureSignal signal(int lineNo,
                                           String rawText,
                                           String normalized,
                                           int indentLevel,
                                           DocumentStructureSignalKind kind,
                                           String code,
                                           String title,
                                           Integer levelHint,
                                           Integer itemIndex,
                                           List<String> reasons,
                                           double confidence) {
        return DocumentStructureSignal.builder()
            .lineNo(lineNo)
            .rawText(rawText)
            .normalizedText(normalized)
            .kind(kind)
            .nodeCode(StrUtil.blankToDefault(code, ""))
            .title(StrUtil.blankToDefault(title, normalized))
            .levelHint(levelHint)
            .indentLevel(indentLevel)
            .itemIndex(itemIndex)
            .reasons(new ArrayList<>(reasons))
            .confidence(confidence)
            .build();
    }

    private String extractCode(String title) {
        Matcher decimal = DECIMAL_HEADING_PATTERN.matcher(title);
        if (decimal.matches()) {
            return decimal.group(1).trim();
        }
        Matcher chapter = CHAPTER_PATTERN.matcher(title);
        if (chapter.matches()) {
            return chapter.group(1).trim();
        }
        Matcher appendix = APPENDIX_PATTERN.matcher(title);
        if (appendix.matches()) {
            return appendix.group(1).trim();
        }
        return "";
    }

    private List<Integer> extractNumericPath(String code) {
        String normalized = safeText(code);
        if (normalized.isBlank()) {
            return List.of();
        }
        if (normalized.contains(".")) {
            List<Integer> path = new ArrayList<>();
            for (String segment : normalized.split("\\.")) {
                if (!segment.chars().allMatch(Character::isDigit)) {
                    return List.of();
                }
                path.add(Integer.parseInt(segment));
            }
            return path;
        }
        Matcher chapter = CHAPTER_PATTERN.matcher(normalized + " 标题");
        if (chapter.find()) {
            Integer chapterNo = parseLooseNumber(chapter.group(2));
            if (chapterNo != null && chapterNo > 0) {
                return List.of(chapterNo);
            }
        }
        return List.of();
    }

    private boolean isTableRow(String normalized) {
        if (normalized.startsWith("|") && normalized.endsWith("|")) {
            return true;
        }
        if (normalized.contains("\t")) {
            return true;
        }
        if (TABLE_SPLIT_PATTERN.split(normalized).length >= 3 && normalized.contains("|")) {
            return true;
        }
        return normalized.matches("^[:\\-\\s|]+$");
    }

    private boolean looksLikePlainHeading(String text,
                                          LineContext context) {
        String normalized = safeText(text);
        if (text.isBlank()) {
            return false;
        }
        if (normalized.length() > properties.getStructureParsing().getMaxPlainHeadingChars()) {
            return false;
        }
        if (endsWithSentencePunctuation(normalized)) {
            return false;
        }
        if (normalized.contains("http://") || normalized.contains("https://")) {
            return false;
        }
        if (normalized.startsWith("|") || normalized.endsWith("|")) {
            return false;
        }
        if (normalized.matches("^[\\-=_]{3,}$")) {
            return false;
        }
        boolean isolated = context.blankBefore() || context.blankAfter();
        boolean nextLooksContent = context.nextNonBlank() != null
            && StrUtil.isNotBlank(context.nextNonBlank().normalizedText())
            && !context.nextNonBlank().normalizedText().matches("^[:\\-\\s|]+$");
        boolean nounLike = !normalized.contains("，")
            && !normalized.contains("；")
            && !normalized.contains("。")
            && !normalized.contains("：")
            && !normalized.toLowerCase(Locale.ROOT).startsWith("http");
        return isolated && nextLooksContent && nounLike;
    }

    private int inferPlainHeadingLevel(LineContext context) {
        if (context == null || context.blankBefore()) {
            return 1;
        }
        return 2;
    }

    private boolean endsWithSentencePunctuation(String text) {
        return text.endsWith("。")
            || text.endsWith("！")
            || text.endsWith("？")
            || text.endsWith("；")
            || text.endsWith(".")
            || text.endsWith("!")
            || text.endsWith("?")
            || text.endsWith(";");
    }

    private List<DocumentStructureLogicalLine> buildLogicalLines(String parsedText) {
        String[] rawLines = StrUtil.blankToDefault(parsedText, "").split("\n", -1);
        List<DocumentStructureLogicalLine> logicalLines = new ArrayList<>(rawLines.length);
        int logicalLineNo = 1;
        for (int index = 0; index < rawLines.length; index++) {
            String rawLine = StrUtil.blankToDefault(rawLines[index], "");
            List<String> segments = splitInlineSegments(rawLine);
            if (segments.isEmpty()) {
                logicalLines.add(new DocumentStructureLogicalLine(
                    logicalLineNo++,
                    index + 1,
                    1,
                    0,
                    rawLine,
                    safeText(rawLine)
                ));
                continue;
            }
            for (int segmentIndex = 0; segmentIndex < segments.size(); segmentIndex++) {
                String segment = segments.get(segmentIndex);
                logicalLines.add(new DocumentStructureLogicalLine(
                    logicalLineNo++,
                    index + 1,
                    segmentIndex + 1,
                    countIndentLevel(segment),
                    segment,
                    safeText(segment)
                ));
            }
        }
        return logicalLines;
    }

    private List<String> splitInlineSegments(String rawLine) {
        if (rawLine == null) {
            return List.of();
        }
        if (rawLine.trim().isEmpty()) {
            return List.of();
        }
        String trimmed = rawLine.trim();
        if (trimmed.startsWith("#")
            || trimmed.startsWith("|")
            || trimmed.startsWith(">")
            || trimmed.matches("^[:\\-\\s|]+$")) {
            return List.of(rawLine);
        }

        List<Integer> boundaries = new ArrayList<>();
        boundaries.add(0);
        Matcher matcher = INLINE_EXPLICIT_STEP_BOUNDARY_PATTERN.matcher(rawLine);
        while (matcher.find()) {
            if (matcher.start() > 0) {
                boundaries.add(matcher.start());
            }
        }
        if (boundaries.size() == 1) {
            return List.of(rawLine);
        }
        List<String> segments = new ArrayList<>();
        for (int index = 0; index < boundaries.size(); index++) {
            int start = boundaries.get(index);
            int end = index == boundaries.size() - 1 ? rawLine.length() : boundaries.get(index + 1);
            String segment = rawLine.substring(start, end).trim();
            if (StrUtil.isNotBlank(segment)) {
                segments.add(segment);
            }
        }
        return segments.isEmpty() ? List.of(rawLine) : segments;
    }

    private int countIndentLevel(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int indent = 0;
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == ' ') {
                indent++;
                continue;
            }
            if (current == '\t') {
                indent += 4;
                continue;
            }
            break;
        }
        return indent;
    }

    private Map<String, Integer> buildLineFrequency(List<DocumentStructureLogicalLine> logicalLines) {
        Map<String, Integer> frequency = new LinkedHashMap<>();
        for (DocumentStructureLogicalLine logicalLine : logicalLines) {
            if (logicalLine == null || StrUtil.isBlank(logicalLine.normalizedText())) {
                continue;
            }
            frequency.merge(logicalLine.normalizedText(), 1, Integer::sum);
        }
        return frequency;
    }

    private LineContext buildContext(List<DocumentStructureLogicalLine> logicalLines, int currentIndex) {
        DocumentStructureLogicalLine previousNonBlank = null;
        boolean blankBefore = false;
        for (int index = currentIndex - 1; index >= 0; index--) {
            DocumentStructureLogicalLine candidate = logicalLines.get(index);
            if (StrUtil.isBlank(candidate.normalizedText())) {
                blankBefore = true;
                continue;
            }
            previousNonBlank = candidate;
            break;
        }
        DocumentStructureLogicalLine nextNonBlank = null;
        boolean blankAfter = false;
        for (int index = currentIndex + 1; index < logicalLines.size(); index++) {
            DocumentStructureLogicalLine candidate = logicalLines.get(index);
            if (StrUtil.isBlank(candidate.normalizedText())) {
                blankAfter = true;
                continue;
            }
            nextNonBlank = candidate;
            break;
        }
        return new LineContext(previousNonBlank, nextNonBlank, blankBefore, blankAfter);
    }

    private boolean isRepeatedNoise(String documentTitle,
                                    String normalized,
                                    int frequency) {
        if (frequency < 2 || StrUtil.isBlank(normalized)) {
            return false;
        }
        if (sameDocumentTitle(documentTitle, normalized)) {
            return true;
        }
        if (COPYRIGHT_NOISE_PATTERN.matcher(normalized).matches()) {
            return true;
        }
        return frequency >= 3
            && normalized.length() <= 120
            && (VERSION_FOOTER_PATTERN.matcher(normalized).matches() || normalized.contains("|"));
    }

    private boolean sameDocumentTitle(String documentTitle,
                                      String candidate) {
        String left = normalizeComparableTitle(documentTitle);
        String right = normalizeComparableTitle(candidate);
        return StrUtil.isNotBlank(left) && left.equals(right);
    }

    private String normalizeComparableTitle(String text) {
        String normalized = safeText(text);
        if (normalized.isBlank()) {
            return "";
        }
        return normalized
            .replaceAll("^#+\\s*", "")
            .replaceAll("\\.[A-Za-z0-9]{1,6}$", "")
            .replaceAll("\\s+", "")
            .toLowerCase(Locale.ROOT);
    }

    private boolean previousIntroducesList(DocumentStructureLogicalLine previousNonBlank) {
        if (previousNonBlank == null) {
            return false;
        }
        String previous = safeText(previousNonBlank.normalizedText());
        return previous.endsWith("：") || previous.endsWith(":");
    }

    private boolean isNeighborSequence(Integer itemIndex,
                                       OrderedMarkerFamily family,
                                       LineContext context) {
        if (itemIndex == null || family == null) {
            return false;
        }
        return isSequenceNeighbor(context.previousNonBlank(), itemIndex, family, -1)
            || isSequenceNeighbor(context.nextNonBlank(), itemIndex, family, 1);
    }

    private boolean isSequenceNeighbor(DocumentStructureLogicalLine candidate,
                                       Integer itemIndex,
                                       OrderedMarkerFamily family,
                                       int offset) {
        if (candidate == null || itemIndex == null) {
            return false;
        }
        Integer candidateIndex = resolveOrderedIndex(candidate.normalizedText(), family);
        return candidateIndex != null && candidateIndex.intValue() == itemIndex.intValue() + offset;
    }

    private Integer resolveOrderedIndex(String text,
                                        OrderedMarkerFamily family) {
        String normalized = safeText(text);
        if (normalized.isBlank()) {
            return null;
        }
        return switch (family) {
            case ARABIC_SINGLE -> {
                Matcher matcher = SINGLE_LEVEL_DIGIT_PATTERN.matcher(normalized);
                yield matcher.matches() ? parseLooseNumber(matcher.group(1)) : null;
            }
            case CHINESE_OUTLINE -> {
                Matcher matcher = CHINESE_OUTLINE_PATTERN.matcher(normalized);
                yield matcher.matches() ? parseLooseNumber(matcher.group(1)) : null;
            }
        };
    }

    private Integer parseLooseNumber(String text) {
        String normalized = safeText(text);
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.chars().allMatch(Character::isDigit)) {
            return Integer.parseInt(normalized);
        }
        Map<Character, Integer> digitMap = Map.of(
            '一', 1, '二', 2, '三', 3, '四', 4, '五', 5,
            '六', 6, '七', 7, '八', 8, '九', 9
        );
        if ("十".equals(normalized)) {
            return 10;
        }
        if (normalized.startsWith("十") && normalized.length() == 2) {
            return 10 + digitMap.getOrDefault(normalized.charAt(1), 0);
        }
        if (normalized.endsWith("十") && normalized.length() == 2) {
            return digitMap.getOrDefault(normalized.charAt(0), 0) * 10;
        }
        if (normalized.contains("十") && normalized.length() == 3) {
            return digitMap.getOrDefault(normalized.charAt(0), 0) * 10 + digitMap.getOrDefault(normalized.charAt(2), 0);
        }
        return digitMap.get(normalized.charAt(0));
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    private enum OrderedMarkerFamily {
        ARABIC_SINGLE,
        CHINESE_OUTLINE
    }

    private record LineContext(
        DocumentStructureLogicalLine previousNonBlank,
        DocumentStructureLogicalLine nextNonBlank,
        boolean blankBefore,
        boolean blankAfter
    ) {
    }
}
