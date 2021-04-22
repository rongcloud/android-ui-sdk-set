package io.rong.imkit.utils;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import io.rong.imkit.R;


public class RongDateUtils {

    private static final int OTHER = 2014;
    private static final int TODAY = 6;
    private static final int YESTERDAY = 15;

    public static int judgeDate(Date date) {
        // 今天
        Calendar calendarToday = Calendar.getInstance();
        calendarToday.set(Calendar.HOUR_OF_DAY, 0);
        calendarToday.set(Calendar.MINUTE, 0);
        calendarToday.set(Calendar.SECOND, 0);
        calendarToday.set(Calendar.MILLISECOND, 0);
        // 昨天
        Calendar calendarYesterday = Calendar.getInstance();
        calendarYesterday.add(Calendar.DATE, -1);
        calendarYesterday.set(Calendar.HOUR_OF_DAY, 0);
        calendarYesterday.set(Calendar.MINUTE, 0);
        calendarYesterday.set(Calendar.SECOND, 0);
        calendarYesterday.set(Calendar.MILLISECOND, 0);

        Calendar calendarTomorrow = Calendar.getInstance();
        calendarTomorrow.add(Calendar.DATE, 1);
        calendarTomorrow.set(Calendar.HOUR_OF_DAY, 0);
        calendarTomorrow.set(Calendar.MINUTE, 0);
        calendarTomorrow.set(Calendar.SECOND, 0);
        calendarTomorrow.set(Calendar.MILLISECOND, 0);

        // 目标日期
        Calendar calendarTarget = Calendar.getInstance();
        calendarTarget.setTime(date);

        if (calendarTarget.before(calendarYesterday)) {// 是否在calendarT之前
            return OTHER;
        } else if (calendarTarget.before(calendarToday)) {
            return YESTERDAY;
        } else if (calendarTarget.before(calendarTomorrow)) {
            return TODAY;
        } else {
            return OTHER;
        }
    }

    private static String getWeekDay(Context context, int dayInWeek) {
        String weekDay = "";
        switch (dayInWeek) {
            case 1:
                weekDay = context.getResources().getString(R.string.rc_date_sunday);
                break;
            case 2:
                weekDay = context.getResources().getString(R.string.rc_date_monday);
                break;
            case 3:
                weekDay = context.getResources().getString(R.string.rc_date_tuesday);
                break;
            case 4:
                weekDay = context.getResources().getString(R.string.rc_date_wednesday);
                break;
            case 5:
                weekDay = context.getResources().getString(R.string.rc_date_thursday);
                break;
            case 6:
                weekDay = context.getResources().getString(R.string.rc_date_friday);
                break;
            case 7:
                weekDay = context.getResources().getString(R.string.rc_date_saturday);
                break;
            default:
                break;
        }
        return weekDay;
    }

    public static boolean isTime24Hour(Context context) {
        String timeFormat = android.provider.Settings.System.getString(context.getContentResolver(),
                android.provider.Settings.System.TIME_12_24);

        return timeFormat != null && timeFormat.equals("24");
    }


    private static String getTimeString(long dateMillis, Context context) {
        if (dateMillis <= 0) {
            return "";
        }

        Date date = new Date(dateMillis);
        String formatTime;
        if (isTime24Hour(context)) {
            formatTime = formatDate(date, "HH:mm");
        } else {
            Calendar calendarTime = Calendar.getInstance();
            calendarTime.setTimeInMillis(dateMillis);
            int hour = calendarTime.get(Calendar.HOUR);
            if (calendarTime.get(Calendar.AM_PM) == Calendar.AM) { //AM
                if (hour < 6) { //凌晨
                    if (hour == 0) {
                        hour = 12;
                    }
                    formatTime = context.getResources().getString(R.string.rc_date_morning);
                } else { //上午
                    formatTime = context.getResources().getString(R.string.rc_date_am);
                }
            } else {//PM
                if (hour == 0) { //中午
                    formatTime = context.getResources().getString(R.string.rc_date_noon);
                    hour = 12;
                } else if (hour <= 5) { //下午
                    formatTime = context.getResources().getString(R.string.rc_date_pm);
                } else {//晚上
                    formatTime = context.getResources().getString(R.string.rc_date_night);
                }
            }

            int minuteInt = calendarTime.get(Calendar.MINUTE);
            String minuteStr = Integer.toString(minuteInt);
            String timeStr;
            if (minuteInt < 10) {
                minuteStr = "0" + minuteStr;
            }
            timeStr = hour + ":" + minuteStr;

            if (context.getResources().getConfiguration().locale.getCountry().equals("CN")) {
                formatTime = formatTime + timeStr;
            } else {
                formatTime = timeStr + " " + formatTime;
            }
        }
        return formatTime;
    }

    private static String getDateTimeString(long dateMillis, boolean showTime, Context context) {
        if (dateMillis <= 0) {
            return "";
        }

        String formatDate = null;

        Date date = new Date(dateMillis);
        int type = judgeDate(date);
        long time = System.currentTimeMillis();
        Calendar calendarCur = Calendar.getInstance();
        Calendar calendardate = Calendar.getInstance();
        calendardate.setTimeInMillis(dateMillis);
        calendarCur.setTimeInMillis(time);
        int month = calendardate.get(Calendar.MONTH);
        int year = calendardate.get(Calendar.YEAR);
        int weekInMonth = calendardate.get(Calendar.WEEK_OF_MONTH);
        int monthCur = calendarCur.get(Calendar.MONTH);
        int yearCur = calendarCur.get(Calendar.YEAR);
        int weekInMonthCur = calendarCur.get(Calendar.WEEK_OF_MONTH);

        switch (type) {
            case TODAY:
                formatDate = getTimeString(dateMillis, context);
                break;

            case YESTERDAY:
                String formatString = context.getResources().getString(R.string.rc_date_yesterday);
                if (showTime) {
                    formatDate = formatString + " " + getTimeString(dateMillis, context);
                } else {
                    formatDate = formatString;
                }
                break;

            case OTHER:
                if (year == yearCur) {//同年
                    if (month == monthCur && weekInMonth == weekInMonthCur) {//同月同周
                        formatDate = getWeekDay(context, calendardate.get(Calendar.DAY_OF_WEEK));
                    } else { //不同月
                        if (context.getResources().getConfiguration().locale.getCountry().equals("CN")) {
                            formatDate = formatDate(date, "M" + context.getResources().getString(R.string.rc_date_month) +
                                    "d" + context.getResources().getString(R.string.rc_date_day));
                        } else {
                            formatDate = formatDate(date, "M/d");
                        }
                    }
                } else {
                    if (context.getResources().getConfiguration().locale.getCountry().equals("CN")) {
                        formatDate = formatDate(date, "yyyy" + context.getResources().getString(R.string.rc_date_year) +
                                "M" + context.getResources().getString(R.string.rc_date_month) +
                                "d" + context.getResources().getString(R.string.rc_date_day));
                    } else {
                        formatDate = formatDate(date, "M/d/yy");
                    }
                }

                if (showTime) {
                    formatDate = formatDate + " " + getTimeString(dateMillis, context);
                }
                break;
            default:
                break;
        }

        return formatDate;
    }


    public static String getConversationListFormatDate(long dateMillis, Context context) {
        return getDateTimeString(dateMillis, false, context);
    }

    public static String getConversationFormatDate(long dateMillis, Context context) {
        return getDateTimeString(dateMillis, true, context);
    }

    /**
     * @param currentTime 当前时间
     * @param preTime     之前的某个时间
     * @param interval    时间间隔
     * @return true 间隔大于interval秒  false 小于等于
     */
    public static boolean isShowChatTime(long currentTime, long preTime, int interval) {

        int typeCurrent = judgeDate(new Date(currentTime));
        int typePre = judgeDate(new Date(preTime));

        if (typeCurrent == typePre) {
            return (currentTime - preTime) > interval * 1000;
        } else {
            return true;
        }
    }

    public static String formatDate(Date date, String fromat) {
        SimpleDateFormat sdf = new SimpleDateFormat(fromat);
        return sdf.format(date);
    }

}
