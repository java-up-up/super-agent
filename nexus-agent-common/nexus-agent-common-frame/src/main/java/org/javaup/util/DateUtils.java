package org.javaup.util;

import lombok.Data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 工具类
 * @author: 阿星不是程序员
 **/

public class DateUtils {

    public static final int WEEK_DAYS = 7;

    public static final int YEAR_MONTHS = 12;

    public static final int DAY_HOURS = 24;

    public static final int HOUR_MINUTES = 60;

    public static final int DAY_MINUTES = 1440;

    public static final int MINUTE_SECONDS = 60;

    public static final int HOUR_SECONDS = 3600;

    public static final int DAY_SECONDS = 86400;

    public static final long SECOND_MILLISECONDS = 1000L;

    public static final long MINUTE_MILLISECONDS = 60000L;

    public static final long HOUR_MILLISECONDS = 3600000L;

    public static final long DAY_MILLISECONDS = 86400000L;

    public static final int WEEK_1_MONDAY = 1;

    public static final int WEEK_2_TUESDAY = 2;

    public static final int WEEK_3_WEDNESDAY = 3;

    public static final int WEEK_4_THURSDAY = 4;

    public static final int WEEK_5_FRIDAY = 5;

    public static final int WEEK_6_SATURDAY = 6;

    public static final int WEEK_7_SUNDAY = 7;

    public static final int MONTH_1_JANUARY = 1;

    public static final int MONTH_2_FEBRUARY = 2;

    public static final int MONTH_3_MARCH = 3;

    public static final int MONTH_4_APRIL= 4;

    public static final int MONTH_5_MAY = 5;

    public static final int MONTH_6_JUNE = 6;

    public static final int MONTH_7_JULY = 7;

    public static final int MONTH_8_AUGUST = 8;

    public static final int MONTH_9_SEPTEMBER = 9;

    public static final int MONTH_10_OCTOBER = 10;

    public static final int MONTH_11_NOVEMBER = 11;

    public static final int MONTH_12_DECEMBER= 12;

    public static final String FORMAT_DATE = "yyyy-MM-dd";

    public static final String FORMAT_HOUR = "yyyy-MM-dd HH";

    public static final String FORMAT_MINUTE = "yyyy-MM-dd HH:mm";

    public static final String FORMAT_SECOND = "yyyy-MM-dd HH:mm:ss";

    public static final String FORMAT_MILLISECOND = "yyyy-MM-dd HH:mm:ss:SSS";

    public static final String FORMAT_NO_DATE = "yyyyMMdd";

    public static final String FORMAT_NO_HOUR = "yyyyMMddHH";

    public static final String FORMAT_NO_MINUTE = "yyyyMMddHHmm";

    public static final String FORMAT_NO_SECOND = "yyyyMMddHHmmss";

    public static final String FORMAT_NO_MILLISECOND = "yyyyMMddHHmmssSSS";

    public static final String FORMAT_UTC = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static Date now(){
        return parseDateTime(getFormatedDateString(8,FORMAT_SECOND));
    }

    public static Date now(String format){
        return parseDateTime(getFormatedDateString(8,format),format);
    }

    public static String nowStr(){
        return getFormatedDateString(8, FORMAT_SECOND);
    }

    public static String nowStr(String pattern){
        return getFormatedDateString(8, pattern);
    }

    private static SimpleDateFormat getSimpleDateFormat(String formatStyle) {
        return new SimpleDateFormat(formatStyle);
    }

    public static String format(Date date, String formatStyle) {
        if (Objects.isNull(date)) {
            return "";
        }
        return getSimpleDateFormat(formatStyle).format(date);
    }

    public static String formatDate(Date date) {
        return format(date, FORMAT_DATE);
    }

    public static String formatDateTime(Date date) {
        return format(date, FORMAT_SECOND);
    }

    public static String formatDateTimeStamp(Date date) {
        return format(date, FORMAT_MILLISECOND);
    }

    public static String formatUtcTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_UTC);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return sdf.format(date);
    }

    public static Date parseDate(String dateString) {
        return parse(dateString, FORMAT_DATE);
    }

    public static Date parseDateTime(String dateTimeStr) {
        return parse(dateTimeStr, FORMAT_SECOND);
    }

    public static Date parseDateTime(String dateTimeStr,String format) {
        return parse(dateTimeStr, format);
    }

    public static Date parseDateTimeStamp(String dateTimeStampStr) {
        return parse(dateTimeStampStr, FORMAT_MILLISECOND);
    }

    public static Date parse(Long timestamp) {
        return new Date(timestamp);
    }

    public static Date parse(String dateString, String formatStyle) {
        String s = getString(dateString);
        if (s.isEmpty()) {
            return null;
        }
        try {
            return getSimpleDateFormat(formatStyle).parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getString(String s) {
        return Objects.isNull(s) ? "" : s.trim();
    }

    public static Date getDateStart(Date date) {
        if (Objects.isNull(date)) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date getDateEnd(Date date) {
        if (Objects.isNull(date)) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    public static int getDateNo(Date date) {
        if (Objects.isNull(date)) {
            return 0;
        }
        return Integer.valueOf(format(date, FORMAT_NO_DATE));
    }

    public static long getDateTimeNo(Date date) {
        if (Objects.isNull(date)) {
            return 0L;
        }
        return Long.parseLong(format(date, FORMAT_NO_SECOND));
    }

    public static long getDateTimeStampNo(Date date) {
        if (Objects.isNull(date)) {
            return 0L;
        }
        return Long.parseLong(format(date, FORMAT_NO_MILLISECOND));
    }

    public static int getWeek(Date date) {
        if (Objects.isNull(date)) {
            return 0;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return getWeek(calendar);
    }

    public static String getWeekStr(Date date) {
        if (Objects.isNull(date)) {
            return "未知";
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return getWeekStr(calendar);
    }

    private static int getWeek(Calendar calendar) {
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
        case Calendar.MONDAY:
            return 1;
        case Calendar.TUESDAY:
            return 2;
        case Calendar.WEDNESDAY:
            return 3;
        case Calendar.THURSDAY:
            return 4;
        case Calendar.FRIDAY:
            return 5;
        case Calendar.SATURDAY:
            return 6;
        case Calendar.SUNDAY:
            return 7;
        default:
            return 0;
        }
    }

    private static String getWeekStr(Calendar calendar) {
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return "周一";
            case Calendar.TUESDAY:
                return "周二";
            case Calendar.WEDNESDAY:
                return "周三";
            case Calendar.THURSDAY:
                return "周四";
            case Calendar.FRIDAY:
                return "周五";
            case Calendar.SATURDAY:
                return "周六";
            case Calendar.SUNDAY:
                return "周日";
            default:
                return "未知";
        }
    }

    public static int getWeekOfYear(Date date) {
        if (Objects.isNull(date)) {
            return -1;
        }
        int weeks = getWeekOfYearIgnoreLastYear(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int week = getWeek(calendar);
        if (week == 1) {
            return weeks;
        }
        return weeks - 1;
    }

    public static int getWeekOfYearIgnoreLastYear(Date date) {
        int seven = 7;
        if (Objects.isNull(date)) {
            return -1;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int days = calendar.get(Calendar.DAY_OF_YEAR);
        int weeks = days / seven;

        if (days % seven == 0) {
            return weeks;
        }

        return weeks + 1;
    }

    public static DateNode getDateNode(Date date) {
        if (Objects.isNull(date)) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        DateNode node = new DateNode();
        node.setTime(format(date, FORMAT_MILLISECOND));
        node.setYear(calendar.get(Calendar.YEAR));
        node.setMonth(calendar.get(Calendar.MONTH) + 1);
        node.setDay(calendar.get(Calendar.DAY_OF_MONTH));
        node.setHour(calendar.get(Calendar.HOUR_OF_DAY));
        node.setMinute(calendar.get(Calendar.MINUTE));
        node.setSecond(calendar.get(Calendar.SECOND));
        node.setMillisecond(calendar.get(Calendar.MILLISECOND));
        node.setWeek(getWeek(calendar));
        node.setDayOfYear(calendar.get(Calendar.DAY_OF_YEAR));
        node.setWeekOfYear(getWeekOfYear(date));
        node.setWeekOfYearIgnoreLastYear(getWeekOfYearIgnoreLastYear(date));
        node.setMillisecondStamp(date.getTime());
        node.setSecondStamp(node.getMillisecondStamp() / 1000);
        return node;
    }

    public static Date add(Date date, int field, int amount) {
        if (Objects.isNull(date)) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(field, amount);
        return calendar.getTime();
    }

    public static Date addYear(Date date, int year) {
        return add(date, Calendar.YEAR, year);
    }

    public static Date addMonth(Date date, int month) {
        return add(date, Calendar.MONTH, month);
    }

    public static Date addDay(Date date, int day) {
        return add(date, Calendar.DAY_OF_YEAR, day);
    }

    public static Date addWeek(Date date, int week) {
        return add(date, Calendar.WEEK_OF_YEAR, week);
    }

    public static Date addHour(Date date, int hour) {
        return add(date, Calendar.HOUR_OF_DAY, hour);
    }

    public static Date addMinute(Date date, int minute) {
        return add(date, Calendar.MINUTE, minute);
    }

    public static Date addSecond(Date date, int second) {
        return add(date, Calendar.SECOND, second);
    }

    public static Date addMillisecond(Date date, int millisecond) {
        return add(date, Calendar.MILLISECOND, millisecond);
    }

    public static Date getWeekDate(Date date, int index) {
        if (index < WEEK_1_MONDAY || index > WEEK_7_SUNDAY) {
            return null;
        }
        int week = getWeek(date);
        return addDay(date, index - week);
    }

    public static Date getWeekDateStart(Date date) {
        return getDateStart(getWeekDate(date, WEEK_1_MONDAY));
    }

    public static Date getWeekDateEnd(Date date) {
        return getWeekDateEnd(getWeekDate(date, WEEK_7_SUNDAY));
    }

    public static List<Date> getWeekDateList(Date date) {
        if (Objects.isNull(date)) {
            return Collections.emptyList();
        }

        Date weekFromDate = getWeekDateStart(date);

        Date weekeEndDate = getWeekDateEnd(date);
        return getBetweenDateList(weekFromDate, weekeEndDate, true);
    }

    public static List<String> getWeekDateList(String dateString) {
        Date date = parseDate(dateString);
        if (Objects.isNull(date)) {
            return Collections.emptyList();
        }
        return getDateStrList(getWeekDateList(date));
    }

    public static List<Date> getMonthDateList(Date date) {
        if (Objects.isNull(date)) {
            return Collections.emptyList();
        }
        Date monthDateStart = getMonthDateStart(date);
        Date monthDateEnd = getMonthDateEnd(date);
        return getBetweenDateList(monthDateStart, monthDateEnd, true);
    }

    public static List<String> getMonthDateList(String dateString) {
        Date date = parseDate(dateString);
        if (Objects.isNull(date)) {
            return Collections.emptyList();
        }
        return getDateStrList(getMonthDateList(date));
    }

    public static Date getMonthDateStart(Date date) {
        if (Objects.isNull(date)) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return getDateStart(calendar.getTime());
    }

    public static Date getMonthDateEnd(Date date) {
        if (Objects.isNull(date)) {
            return null;
        }
        Date monthDateStart = getMonthDateStart(date);
        Date nextMonthDateStart = getMonthDateStart(addMonth(monthDateStart, 1));
        return getDateEnd(addDay(nextMonthDateStart, -1));
    }

    public static long countBetweenSecond(Date date1, Date date2) {
        if (Objects.isNull(date1) || Objects.isNull(date2)) {
            return -1;
        }

        long diffInMilliseconds = Math.abs(date2.getTime() - date1.getTime());
        return diffInMilliseconds / 1000;
    }

    public static List<Date> getBetweenDateList(Date date1, Date date2, boolean isContainParams) {
        if (Objects.isNull(date1) || Objects.isNull(date2)) {
            return Collections.emptyList();
        }

        Date fromDate = date1;
        Date toDate = date2;
        if (date2.before(date1)) {
            fromDate = date2;
            toDate = date1;
        }

        Date from = getDateStart(fromDate);
        Date to = getDateStart(toDate);

        List<Date> dates = new ArrayList<Date>();
        if (isContainParams) {
            dates.add(from);
        }
        Date date = from;
        boolean isBefore = true;
        while (isBefore) {
            date = addDay(date, 1);
            isBefore = date.before(to);
            if (isBefore) {
                dates.add(getDateStart(date));
            }
        }
        if (isContainParams) {
            dates.add(to);
        }
        return dates;
    }

    public static List<String> getBetweenDateList(String dateString1, String dateString2) {
        return getBetweenDateList(dateString1, dateString2, false);
    }

    public static List<String> getBetweenDateList(String dateString1, String dateString2, boolean isContainParams) {
        Date date1 = parseDate(dateString1);
        Date date2 = parseDate(dateString2);
        List<Date> dates = getBetweenDateList(date1, date2, isContainParams);
        return getDateStrList(dates);
    }

    public static List<String> getDateStrList(List<Date> dates) {
        if (dates.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> dateList = new ArrayList<String>();
        for (Date date : dates) {
            dateList.add(formatDate(date));
        }
        return dateList;
    }

    public static String getFormatedDateString(float timeZoneOffset, String pattern) {
        int thirteen = 13;
        int minusTwelve = -12;
        if (timeZoneOffset > thirteen || timeZoneOffset < minusTwelve) {
            timeZoneOffset = 0;
        }

        int newTime = (int) (timeZoneOffset * 60 * 60 * 1000);
        TimeZone timeZone;
        String[] ids = TimeZone.getAvailableIDs(newTime);
        if (ids.length == 0) {
            timeZone = TimeZone.getDefault();
        } else {
            timeZone = new SimpleTimeZone(newTime, ids[0]);
        }

        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setTimeZone(timeZone);
        return sdf.format(new Date());
    }
    @Data
    static class DateNode {

        private int year;

        private int month;

        private int day;

        private int hour;

        private int minute;

        private int second;

        private int millisecond;

        private int week;

        private int dayOfYear;

        private int weekOfYear;

        private int weekOfYearIgnoreLastYear;

        private long secondStamp;

        private long millisecondStamp;

        private String time;

    }

    public static Date getDate(String dateStr, String pattern) {
        return getDate(dateStr, pattern, null);
    }

    public static Date getDate(String dateStr, String pattern, Date defaultDate) {
        if (dateStr != null && pattern != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                return sdf.parse(dateStr);
            } catch (ParseException e) {
                throw new IllegalArgumentException("字符串转化为日期失败！", e);
            }
        }
        return defaultDate;
    }
}
