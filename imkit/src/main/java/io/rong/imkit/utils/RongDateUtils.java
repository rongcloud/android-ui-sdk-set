package io.rong.imkit.utils;

import android.content.Context;
import android.text.TextUtils;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.utils.language.LangUtils;
import io.rong.imkit.utils.language.RongConfigurationManager;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RongDateUtils {

    private static final String TAG = RongDateUtils.class.getCanonicalName();
    private static final String SPACE_CHAR = " ";
    private static final int OTHER = 2014;
    private static final int TODAY = 6;
    private static final int YESTERDAY = 15;

    public static int judgeDate(Context context, Date date) {
        Locale locale = LangUtils.getAppLanguageLocal(context).toLocale();
        // 今天
        Calendar calendarToday = Calendar.getInstance(locale);
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
        Calendar calendarTarget = Calendar.getInstance(locale);
        calendarTarget.setTime(date);

        if (calendarTarget.before(calendarYesterday)) { // 是否在calendarT之前
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
        String timeFormat =
                android.provider.Settings.System.getString(
                        context.getContentResolver(), android.provider.Settings.System.TIME_12_24);

        return timeFormat != null && timeFormat.equals("24");
    }

    private static String getTimeString(long dateMillis, Context context) {
        if (dateMillis <= 0) {
            return "";
        }

        Date date = new Date(dateMillis);
        String formatTime;
        if (isTime24Hour(context)) {
            formatTime = formatDate(context, date, "HH:mm");
        } else {
            formatTime = formatDate(context, date, "h:mm");
            if (RongConfigurationManager.getInstance().getLanguageLocal(context)
                    == LangUtils.RCLocale.LOCALE_CHINA) {
                formatTime = getTime12HourDes(dateMillis, context) + SPACE_CHAR + formatTime;
            } else {
                formatTime = formatTime + SPACE_CHAR + getTime12HourDes(dateMillis, context);
            }
        }
        return formatTime;
    }

    private static String getTime12HourDes(long dateMillis, Context context) {
        Calendar calendarTime =
                Calendar.getInstance(LangUtils.getAppLanguageLocal(context).toLocale());
        calendarTime.setTimeInMillis(dateMillis);
        int hour = calendarTime.get(Calendar.HOUR);
        String des = "";
        if (calendarTime.get(Calendar.AM_PM) == Calendar.AM) { // AM
            if (hour < 6) { // 凌晨
                des = context.getResources().getString(R.string.rc_date_morning);
            } else { // 上午
                des = context.getResources().getString(R.string.rc_date_am);
            }
        } else { // PM
            if (hour == 0) { // 中午
                des = context.getResources().getString(R.string.rc_date_noon);
            } else if (hour <= 5) { // 下午
                des = context.getResources().getString(R.string.rc_date_pm);
            } else { // 晚上
                des = context.getResources().getString(R.string.rc_date_night);
            }
        }
        return des;
    }

    private static String getDateTimeString(long dateMillis, boolean showTime, Context context) {
        if (dateMillis <= 0) {
            return "";
        }

        String formatDate = null;

        Date date = new Date(dateMillis);
        int type = judgeDate(context, date);
        long time = System.currentTimeMillis();
        Locale locale = LangUtils.getAppLanguageLocal(context).toLocale();
        Calendar calendarCur = Calendar.getInstance(locale);
        Calendar calendardate = Calendar.getInstance(locale);
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
                if (year == yearCur) { // 同年
                    if (month == monthCur && weekInMonth == weekInMonthCur) { // 同月同周
                        formatDate = getWeekDay(context, calendardate.get(Calendar.DAY_OF_WEEK));
                    } else { // 不同月
                        formatDate = formatDate(context, date, "M/d");
                    }
                } else {
                    formatDate = formatDate(context, date, "yyyy/M/d");
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
     * @param preTime 之前的某个时间
     * @param interval 时间间隔
     * @return true 间隔大于interval秒 false 小于等于
     */
    public static boolean isShowChatTime(
            Context context, long currentTime, long preTime, int interval) {

        int typeCurrent = judgeDate(context, new Date(currentTime));
        int typePre = judgeDate(context, new Date(preTime));

        if (typeCurrent == typePre) {
            return (currentTime - preTime) > interval * 1000;
        } else {
            return true;
        }
    }

    public static String formatDate(Context context, Date date, String fromat) {
        if (TextUtils.isEmpty(fromat)) {
            return "";
        }

        try {
            SimpleDateFormat sdf =
                    new SimpleDateFormat(fromat, LangUtils.getAppLanguageLocal(context).toLocale());
            return sdf.format(date);
        } catch (IllegalArgumentException e1) {
            RLog.e(TAG, "the given pattern is invalid.");
        }

        return "";
    }
}
