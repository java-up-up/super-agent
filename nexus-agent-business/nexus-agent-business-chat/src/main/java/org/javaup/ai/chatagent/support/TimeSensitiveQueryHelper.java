package org.javaup.ai.chatagent.support;

import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 支撑组件
 * @author: 阿星不是程序员
 **/

public final class TimeSensitiveQueryHelper {

    private static final Pattern EXPLICIT_DATE_PATTERN = Pattern.compile(
        "(\\d{4}[-/.年]\\d{1,2}[-/.月]\\d{1,2}日?)|(\\d{1,2}月\\d{1,2}日)"
    );

    private static final List<String> RELATIVE_TIME_KEYWORDS = List.of(
        "今天", "今日", "明天", "明日", "昨天", "昨日", "后天", "前天",
        "现在", "当前", "目前", "此刻", "实时", "最新", "刚刚",
        "本周", "这周", "本月", "这个月", "今年", "本年度", "本季度",
        "周几", "星期几", "几号", "日期", "几月几号"
    );

    private static final List<String> FRESH_INFORMATION_KEYWORDS = List.of(
        "天气", "气温", "温度", "降雨", "下雨", "下雪", "空气质量", "aqi",
        "限号", "限行", "尾号限行",
        "汇率", "金价", "黄金价格", "银价", "油价",
        "股价", "行情", "大盘", "指数",
        "新闻", "头条", "热搜", "热榜",
        "路况", "拥堵",
        "票房", "排片",
        "航班", "班次", "列车", "高铁", "火车", "地铁运营",
        "比分", "赛果", "赛程", "比赛结果",
        "预警", "台风"
    );

    private static final List<String> CALENDAR_KEYWORDS = List.of(
        "周几", "星期几", "几号", "日期", "几月几号", "星期", "周"
    );

    private static final List<String> HISTORICAL_HINTS = List.of(
        "历史", "过去", "去年", "前年", "上周", "上个月", "上月", "上一周",
        "上一月", "往年", "历年", "当时", "之前", "回顾", "曾经"
    );

    private TimeSensitiveQueryHelper() {
    }

    public static boolean requiresCurrentDateAnchoring(String query) {
        if (StrUtil.isBlank(query)) {
            return false;
        }
        if (hasHistoricalIntent(query) && !hasRelativeTimeReference(query) && !looksCalendarQuestion(query)) {
            return false;
        }
        return hasRelativeTimeReference(query)
            || looksCurrentInfoDomain(query)
            || looksCalendarQuestion(query);
    }

    public static boolean requiresFreshSearch(String query) {
        if (StrUtil.isBlank(query)) {
            return false;
        }
        if (hasHistoricalIntent(query) || containsExplicitDate(query)) {
            return false;
        }
        if (looksCalendarQuestion(query)) {
            return false;
        }

        String normalized = normalize(query);
        return looksCurrentInfoDomain(normalized)
            || containsAny(normalized, "最新", "实时", "当前", "现在", "目前", "刚刚");
    }

    public static String buildEffectiveSearchQuery(String query, String currentDate) {
        if (StrUtil.isBlank(query)) {
            return query;
        }

        String trimmedQuery = query.trim();
        if (StrUtil.isBlank(currentDate)) {
            return trimmedQuery;
        }
        if (!requiresCurrentDateAnchoring(trimmedQuery)) {
            return trimmedQuery;
        }
        if (containsExplicitDate(trimmedQuery) || trimmedQuery.contains(currentDate) || hasHistoricalIntent(trimmedQuery)) {
            return trimmedQuery;
        }
        return trimmedQuery + " " + currentDate + " " + deriveTemporalHint(trimmedQuery);
    }

    public static boolean containsExplicitDate(String query) {
        return StrUtil.isNotBlank(query) && EXPLICIT_DATE_PATTERN.matcher(query).find();
    }

    public static boolean hasRelativeTimeReference(String query) {
        return containsAny(normalize(query), RELATIVE_TIME_KEYWORDS);
    }

    public static boolean looksCalendarQuestion(String query) {
        return containsAny(normalize(query), CALENDAR_KEYWORDS);
    }

    public static boolean looksCurrentInfoDomain(String query) {
        return containsAny(normalize(query), FRESH_INFORMATION_KEYWORDS);
    }

    public static boolean hasHistoricalIntent(String query) {
        return containsAny(normalize(query), HISTORICAL_HINTS);
    }

    private static String deriveTemporalHint(String query) {
        String normalized = normalize(query);
        if (containsAny(normalized, "明天", "明日")) {
            return "明天";
        }
        if (containsAny(normalized, "昨天", "昨日", "前天")) {
            return "昨天";
        }
        if (containsAny(normalized, "本周", "这周")) {
            return "本周";
        }
        if (containsAny(normalized, "本月", "这个月")) {
            return "本月";
        }
        if (containsAny(normalized, "今年", "本年度", "本季度")) {
            return "今年";
        }
        if (containsAny(normalized, "最新", "实时", "当前", "现在", "目前", "刚刚")) {
            return "最新";
        }
        return "今天";
    }

    private static String normalize(String query) {
        return StrUtil.isNotBlank(query) ? query.trim().toLowerCase(Locale.ROOT) : "";
    }

    private static boolean containsAny(String query, List<String> candidates) {
        if (StrUtil.isBlank(query)) {
            return false;
        }
        for (String candidate : candidates) {
            if (query.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String query, String... candidates) {
        if (StrUtil.isBlank(query)) {
            return false;
        }
        for (String candidate : candidates) {
            if (query.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
