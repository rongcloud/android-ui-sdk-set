package io.rong.imkit.picture.tools;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateUtils {
    private SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd_HHmmssSS");

    private DateUtils() {

    }

    private static class SingletonHolder {
        static DateUtils sInstance = new DateUtils();
    }
    
    public static DateUtils getInstance() {
        return SingletonHolder.sInstance;
    }

    /**
     * 判断两个时间戳相差多少秒
     *
     * @param d
     * @return
     */
    public int dateDiffer(long d) {
        try {
            long l1 = Long.parseLong(String.valueOf(System.currentTimeMillis()).substring(0, 10));
            long interval = l1 - d;
            return (int) Math.abs(interval);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 时间戳转换成时间格式
     *
     * @param duration
     * @return
     */
    public String formatDurationTime(long duration) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
    }


    /**
     * 根据时间戳创建文件名
     *
     * @param prefix 前缀名
     * @return
     */
    public String getCreateFileName(String prefix) {
        long millis = System.currentTimeMillis();
        return prefix + sf.format(millis);
    }

    /**
     * 计算两个时间间隔
     *
     * @param sTime
     * @param eTime
     * @return
     */
    public String cdTime(long sTime, long eTime) {
        long diff = eTime - sTime;
        return diff > 1000 ? diff / 1000 + "s" : diff + "ms";
    }
}
