package io.rong.imkit.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import io.rong.imkit.R;
import io.rong.imlib.model.Conversation;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Created by DragonJ on 15/3/25. */
public class TimeUtils {

    public static String formatData(Context context, long timeMillis) {

        if (timeMillis == 0) return "";

        String result;

        int targetDay = (int) (timeMillis / (24 * 60 * 60 * 1000));
        int nowDay = (int) (System.currentTimeMillis() / (24 * 60 * 60 * 1000));

        if (targetDay == nowDay) {
            result = fromatDate(timeMillis, "HH:mm");
        } else if (targetDay + 1 == nowDay) {
            String formatString = context.getResources().getString(R.string.rc_yesterday_format);
            result = String.format(formatString, fromatDate(timeMillis, "HH:mm"));
        } else {
            result = fromatDate(timeMillis, "yyyy-MM-dd");
        }

        return result;
    }

    public static String formatTime(Context context, long timeMillis) {

        if (timeMillis == 0) return "";

        String result;

        int targetDay = (int) (timeMillis / (24 * 60 * 60 * 1000));
        int nowDay = (int) (System.currentTimeMillis() / (24 * 60 * 60 * 1000));

        if (targetDay == nowDay) {
            result = fromatDate(timeMillis, "HH:mm");
        } else if (targetDay + 1 == nowDay) {
            String formatString = context.getResources().getString(R.string.rc_yesterday_format);
            result = String.format(formatString, fromatDate(timeMillis, "HH:mm"));
        } else {
            result = fromatDate(timeMillis, "yyyy-MM-dd HH:mm");
        }

        return result;
    }

    private static String fromatDate(long timeMillis, String fromat) {
        SimpleDateFormat sdf = new SimpleDateFormat(fromat);
        return sdf.format(new Date(timeMillis));
    }

    public static long getLatestTime(@NonNull Conversation conversation) {
        if (conversation.getLatestMessage() != null) {
            return conversation.getSentTime();
        }
        return conversation.getOperationTime();
    }
}
