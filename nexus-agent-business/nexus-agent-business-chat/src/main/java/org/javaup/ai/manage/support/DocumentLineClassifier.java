package org.javaup.ai.manage.support;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 支撑组件
 * @author: 阿星不是程序员
 **/

@Component
public class DocumentLineClassifier {

    private static final Pattern MARKDOWN_HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern MULTI_LEVEL_DIGIT_HEADING_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)+)\\s*[、.]?\\s*(.+)$");
    private static final Pattern SINGLE_LEVEL_DIGIT_LINE_PATTERN = Pattern.compile("^(\\d+)\\s*[、.]\\s*(.+)$");
    private static final Pattern CHINESE_CHAPTER_PATTERN = Pattern.compile("^(第[一二三四五六七八九十百\\d]+[章节条部分])\\s*(.+)$");
    private static final Pattern CHINESE_OUTLINE_PATTERN = Pattern.compile("^([一二三四五六七八九十百]+)[、.]\\s*(.+)$");
    private static final Pattern APPENDIX_PATTERN = Pattern.compile("^(附录\\s*[A-Za-z一二三四五六七八九十百\\d]+)(?:\\s+(.+))?$");
    private static final Pattern EXPLICIT_STEP_PATTERN = Pattern.compile("^(?:第\\s*([0-9一二三四五六七八九十百]+)\\s*步|步骤\\s*([0-9一二三四五六七八九十百]+))\\s*[:：、.]?\\s*(.+)$");

    public LineClassification classify(String line) {
        String normalized = safeText(line);
        if (normalized.isBlank()) {
            return new LineClassification(LineKind.BODY, 0, normalized, normalized);
        }

        Matcher markdownMatcher = MARKDOWN_HEADING_PATTERN.matcher(normalized);
        if (markdownMatcher.matches()) {
            int level = markdownMatcher.group(1).length();
            return heading(level, markdownMatcher.group(2).trim(), normalized);
        }

        Matcher appendixMatcher = APPENDIX_PATTERN.matcher(normalized);
        if (appendixMatcher.matches()) {
            return heading(1, normalized, normalized);
        }

        Matcher explicitStepMatcher = EXPLICIT_STEP_PATTERN.matcher(normalized);
        if (explicitStepMatcher.matches()) {
            return listItem(normalized);
        }

        Matcher chapterMatcher = CHINESE_CHAPTER_PATTERN.matcher(normalized);
        if (chapterMatcher.matches()) {
            return heading(2, normalized, normalized);
        }

        Matcher multiLevelDigitMatcher = MULTI_LEVEL_DIGIT_HEADING_PATTERN.matcher(normalized);
        if (multiLevelDigitMatcher.matches()) {
            String prefix = multiLevelDigitMatcher.group(1);
            return heading(prefix.split("\\.").length, normalized, normalized);
        }

        Matcher chineseOutlineMatcher = CHINESE_OUTLINE_PATTERN.matcher(normalized);
        if (chineseOutlineMatcher.matches()) {
            String content = chineseOutlineMatcher.group(2).trim();
            if (looksLikeHeadingContent(content)) {
                return heading(1, normalized, normalized);
            }
            return listItem(normalized);
        }

        Matcher singleLevelDigitMatcher = SINGLE_LEVEL_DIGIT_LINE_PATTERN.matcher(normalized);
        if (singleLevelDigitMatcher.matches()) {
            String content = singleLevelDigitMatcher.group(2).trim();
            if (looksLikeHeadingContent(content)) {
                return heading(1, normalized, normalized);
            }
            return listItem(normalized);
        }

        if (normalized.startsWith("- ")
            || normalized.startsWith("* ")
            || normalized.startsWith("+ ")
            || normalized.startsWith("- [")
            || normalized.startsWith("* [")
            || normalized.startsWith("+ [")) {
            return listItem(normalized);
        }

        return new LineClassification(LineKind.BODY, 0, normalized, normalized);
    }

    private LineClassification heading(int level, String title, String rawText) {
        return new LineClassification(LineKind.HEADING, Math.max(level, 1), safeText(title), safeText(rawText));
    }

    private LineClassification listItem(String rawText) {
        return new LineClassification(LineKind.LIST_ITEM, 0, safeText(rawText), safeText(rawText));
    }

    private boolean looksLikeHeadingContent(String content) {
        String normalized = safeText(content);
        if (normalized.isBlank()) {
            return false;
        }

        if (endsWithSentencePunctuation(normalized)) {
            return false;
        }
        if (normalized.length() > 24) {
            return false;
        }
        return !normalized.contains("，")
            && !normalized.contains("；")
            && !normalized.contains("。")
            && !normalized.contains("：");
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

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    public enum LineKind {
        HEADING,
        LIST_ITEM,
        BODY
    }

    public record LineClassification(
        LineKind kind,
        int level,
        String title,
        String rawText
    ) {
        public boolean isHeading() {
            return kind == LineKind.HEADING;
        }

        public boolean isListItem() {
            return kind == LineKind.LIST_ITEM;
        }
    }
}
