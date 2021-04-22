package io.rong.imkit.feature.forward;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.rong.common.FileUtils;
import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.utils.FileTypeUtils;
import io.rong.imkit.utils.RongDateUtils;
import io.rong.imkit.utils.RongUtils;
import io.rong.imlib.MessageTag;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UserInfo;
import io.rong.message.FileMessage;
import io.rong.message.GIFMessage;
import io.rong.message.ImageMessage;
import io.rong.imlib.location.message.LocationMessage;
import io.rong.message.SightMessage;
import io.rong.message.TextMessage;
import io.rong.message.utils.BitmapUtil;

/**
 * 读取template.json模板,根据模板将消息队列转为网页文本,并生成文件
 */
public class CombineMessageUtils {
    private static final String TAG = CombineMessageUtils.class.getSimpleName();
    private static final String COMBINE_FILE_PATH = "combine";
    private static final String COMBINE_FILE_SUFFIX = ".html";

    // 头像长宽大于100,等比例压缩为100以内
    private static final int IMAGE_WIDTH = 100;
    private static final int IMAGE_HEIGHT = 100;

    private static final String JSON_FILE_NAME = "combine.json";// 模板文件
    private static final String BASE64_PRE = "data:image/jpeg;base64,";// base64前缀
    private static final String NO_USER = "rong-none-user"; // 不显示用户头像标识

    // 消息类型
    private static final String TAG_BASE_HEAD = "baseHead"; // html头
    private static final String TAG_TIME = "time"; // 时间
    private static final String TAG_TXT = "RC:TxtMsg"; // 文本
    private static final String TAG_GIF = "RC:GIFMsg"; // 动图
    private static final String TAG_VC = "RC:VcMsg"; // 语音
    private static final String TAG_HQVC = "RC:HQVCMsg"; // 语音
    private static final String TAG_CARD = "RC:CardMsg"; // 名片
    private static final String TAG_STK = "RC:StkMsg"; // 动态表情
    private static final String TAG_IMG_TEXT = "RC:ImgTextMsg";// 图文
    private static final String TAG_SIGHT = "RC:SightMsg"; // 小视频
    private static final String TAG_IMG = "RC:ImgMsg"; // 图片
    private static final String TAG_COMBINE = "RC:CombineMsg"; // 合并
    private static final String TAG_MSG_COMBINE_BODY = "CombineMsgBody"; // 合并消息简略信息
    private static final String TAG_FILE = "RC:FileMsg"; // 文件
    private static final String TAG_LBS = "RC:LBSMsg"; // 位置
    private static final String TAG_VCSUMMARY = "RC:VCSummary"; // 音视频通话
    private static final String TAG_VST = "RC:VSTMsg"; // 音视频通话
    private static final String TAG_RP = "RCJrmf:RpMsg"; // 红包
    private static final String TAG_BASE_BOTTOM = "baseBottom"; // html底

    // 消息参数
    private static final String MSG_BASE_HEAD_STYLE = "{%style%}"; // 用户自定义样式
    private static final String MSG_TIME = "{%time%}"; // 时间
    private static final String MSG_SHOW_USER = "{%showUser%}";// 是否显示用户信息,不显示传rong-none-user.显示传'';
    private static final String MSG_PORTRAIT = "{%portrait%}"; // 头像(url或base64)
    private static final String MSG_USER_NAMEM = "{%userName%}"; // 用户名称
    private static final String MSG_SEND_TIME = "{%sendTime%}"; // 发送时间
    private static final String MSG_TEXT = "{%text%}"; // 各个消息的文本
    private static final String MSG_IMAG_URL = "{%imgUrl%}"; // 图片链接(url或base64)
    private static final String MSG_FILE_NAME = "{%fileName%}"; // 文件名称
    private static final String MSG_SIZE = "{%size%}"; // 文件大小(xxxk/m/g)
    private static final String MSG_FILE_SIZE = "{%fileSize%}"; // 文件大小(具体数值)
    private static final String MSG_FILE_URL = "{%fileUrl%}"; // 文件链接
    private static final String MSG_FILE_TYPE = "{%fileType%}"; // 文件类型
    private static final String MSG_FILE_ICON = "{%fileIcon%}"; // 文件图标
    private static final String MSG_TITLE = "{%title%}"; // 标题
    private static final String MSG_COMBINE_BODY = "{%combineBody%}"; // 合并消息简略信息
    private static final String MSG_FOOT = "{%foot%}"; // 合并消息底部显示文本
    private static final String MSG_LOCATION_NAME = "{%locationName%}"; // 位置信息
    private static final String MSG_LATITUDE = "{%latitude%}"; // 纬度
    private static final String MSG_LONGITTUDE = "{%longitude%}"; // 经度
    private static final String MSG_IMAGE_BASE64 = "{%imageBase64%}";
    private static final String MSG_DURATION = "{%duration%}";

    private Map<String, String> DATA = new HashMap<>();
    private Uri URI = null;
    private Boolean isSameDay;
    private Boolean isSameYear;
    private String style = "";
    private String sendUserId;

    private CombineMessageUtils() {
    }

    private static class Holder {
        private static CombineMessageUtils Utils = new CombineMessageUtils();
    }

    public static CombineMessageUtils getInstance() {
        return Holder.Utils;
    }

    /**
     * 为合并消息拼接网页文本,并生成文件.
     *
     * @return 文件地址
     */
    Uri getUrlFromMessageList(List<Message> messagesList) {
        style = "";
        URI = null;
        isSameDay = isSameYear = false;
        String filePath = FileUtils.getCachePath(IMCenter.getInstance().getContext())
                + File.separator + COMBINE_FILE_PATH
                + File.separator + System.currentTimeMillis() + COMBINE_FILE_SUFFIX;
        String filStr = getHtmlFromMessageList(messagesList);
        FileUtils.saveFile(filStr, filePath);
        return Uri.parse("file://" + filePath);
    }

    // 拼接html文本
    private String getHtmlFromMessageList(List<Message> messagesList) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getHtmlBaseHead());// 加载html头部

        stringBuilder.append(getHtmlTime(messagesList));// 加载html头部时间
        for (Message msg : messagesList) {
            stringBuilder.append(getHtmlFromMessageContent(msg, msg.getContent()));
        }

        stringBuilder.append(getHtmlBaseBottom());// 加载html底部
        return stringBuilder.toString();
    }

    private String getHtmlBaseHead() {
        return getHtmlFromType(TAG_BASE_HEAD).replace(MSG_BASE_HEAD_STYLE, style);
    }

    private String getHtmlTime(List<Message> messagesList) {
        long first = messagesList.get(0).getSentTime();
        Calendar firstCalendar = Calendar.getInstance();
        firstCalendar.setTimeInMillis(first);

        long last = messagesList.get(messagesList.size() - 1).getSentTime();
        Calendar lastCalendar = Calendar.getInstance();
        lastCalendar.setTimeInMillis(last);

        isSameYear = firstCalendar.get(Calendar.YEAR) == lastCalendar.get(Calendar.YEAR);
        isSameDay = isSameYear
                && firstCalendar.get(Calendar.MONTH) == lastCalendar.get(Calendar.MONTH)
                && firstCalendar.get(Calendar.DAY_OF_MONTH) == lastCalendar.get(Calendar.DAY_OF_MONTH);

        String time;
        String format = "yyyy-M-d";
        if (isSameDay) {
            time = new SimpleDateFormat(format, Locale.CANADA).format(first);
        } else {
            time = new SimpleDateFormat(format, Locale.CANADA).format(first)
                    + " - " + new SimpleDateFormat(format, Locale.CANADA).format(last);
        }
        return getHtmlFromType(TAG_TIME).replace(MSG_TIME, time);
    }

    private String getHtmlFromMessageContent(Message message, MessageContent content) {
        MessageTag tag = content.getClass().getAnnotation(MessageTag.class);
        if (tag == null || !tag.value().startsWith("RC:")) {
            RLog.e(TAG, "getHtmlFromMessageContent tag is UnKnown, content:" + content);
            return "";
        }
        String type = tag.value();
        String html = setUserInfo(getHtmlFromType(type), message);
        switch (type) {
            case TAG_TXT: // 文本
                TextMessage text = (TextMessage) content;
                html = html.replace(MSG_TEXT, text.getContent());
                break;
            case TAG_IMG_TEXT: // 图文
            case TAG_VC: // 语音
            case TAG_HQVC: // 语音
                html = html.replace(MSG_TEXT, getSpannable(content));
                break;
            case TAG_STK: // 表情
                html = html.replace(MSG_TEXT, IMCenter.getInstance().getContext().getString(R.string.rc_message_content_sticker));
                break;
            case TAG_CARD:// 名片
                html = html.replace(MSG_TEXT, IMCenter.getInstance().getContext().getString(R.string.rc_message_content_card));
                break;
            case TAG_VST:// 音视频通话
            case TAG_VCSUMMARY:// 音视频通话
                html = html.replace(MSG_TEXT, IMCenter.getInstance().getContext().getString(R.string.rc_message_content_vst));
                break;
            case TAG_RP:// 红包
                html = html.replace(MSG_TEXT, IMCenter.getInstance().getContext().getString(R.string.rc_message_content_rp));
                break;
            case TAG_SIGHT: // 小视频
                SightMessage sight = (SightMessage) content;
                String sightBase64 = getBase64FromUrl(sight.getThumbUri());
                int duration = sight.getDuration();
                html = html.replace(MSG_FILE_NAME, sight.getName())
                        .replace(MSG_SIZE, FileTypeUtils.formatFileSize(sight.getSize()))
                        .replace(MSG_FILE_URL, sight.getMediaUrl() == null ? "" : sight.getMediaUrl().toString())
                        .replace(MSG_IMAGE_BASE64, sightBase64)
                        .replace(MSG_DURATION, String.valueOf(duration));

                break;
            case TAG_IMG: // 图片
                ImageMessage image = (ImageMessage) content;
                String base64 = getBase64FromUrl(image.getThumUri());
                html = html.replace(MSG_FILE_URL, image.getMediaUrl() == null ? "" : image.getMediaUrl().toString())
                        .replace(MSG_IMAG_URL, base64);
                break;
            case TAG_GIF: // gif图片
                GIFMessage gif = (GIFMessage) content;
                String gifBase64 = getBase64FromUrl(gif.getRemoteUri());
                html = html.replace(MSG_FILE_URL, gif.getRemoteUri() == null ? "" : gif.getRemoteUri().toString())
                        .replace(MSG_IMAG_URL, gifBase64);
                break;
            case TAG_FILE: // 文件
                FileMessage file = (FileMessage) content;
                html = html.replace(MSG_FILE_NAME, file.getName())
                        .replace(MSG_SIZE, FileTypeUtils.formatFileSize(file.getSize()))
                        .replace(MSG_FILE_SIZE, String.valueOf(file.getSize()))
                        .replace(MSG_FILE_URL, file.getFileUrl() == null ? "" : file.getFileUrl().toString())
                        .replace(MSG_FILE_TYPE, file.getType())
                        .replace(MSG_FILE_ICON, getBase64FromImageId(FileTypeUtils.fileTypeImageId(IMCenter.getInstance().getContext(), file.getName())));
                break;
            case TAG_LBS: // 位置
                LocationMessage location = (LocationMessage) content;
                html = html.replace(MSG_LOCATION_NAME, location.getPoi())
                        .replace(MSG_LATITUDE, String.valueOf(location.getLat()))
                        .replace(MSG_LONGITTUDE, String.valueOf(location.getLng()));
                break;
            case TAG_COMBINE: // 合并
                CombineMessage combine = (CombineMessage) content;
                StringBuilder summary = new StringBuilder();
                String combineBody = getHtmlFromType(TAG_MSG_COMBINE_BODY);
                List<String> summarys = combine.getSummaryList();
                for (String sum : summarys) {
                    summary.append(combineBody.replace(MSG_TEXT, sum));
                }
                html = html.replace(MSG_FILE_URL, combine.getMediaUrl() == null ? "" : combine.getMediaUrl().toString())
                        .replace(MSG_TITLE, combine.getTitle())
                        .replace(MSG_COMBINE_BODY, summary.toString())
                        .replace(MSG_FOOT, IMCenter.getInstance().getContext().getString(R.string.rc_combine_chat_history));
                break;
            default:
                RLog.e(TAG, "getHtmlFromMessageContent UnKnown type:" + type);
        }
        return html;
    }

    private String getHtmlBaseBottom() {
        return getHtmlFromType(TAG_BASE_BOTTOM);
    }

    private String getHtmlFromType(String type) {
        if (DATA == null || DATA.size() == 0) {
            DATA = getDATA();
        }

        if (DATA == null || DATA.size() == 0) {
            RLog.e(TAG, "getHtmlFromType data is null");
            return "";
        }

        if (TAG_HQVC.equals(type)) {
            type = TAG_VC;
        }

        if (TAG_VST.equals(type)) {
            type = TAG_VCSUMMARY;
        }

        String html = DATA.get(type);
        if (TextUtils.isEmpty(html)) {
            RLog.e(TAG, "getHtmlFromType html is null, type:" + type);
            return "";
        }
        return html;
    }

    private Map<String, String> getDATA() {
        DATA = setData(getJson());
        return DATA;
    }

    private String getJson() {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bf = null;
        try {
            bf = new BufferedReader(new InputStreamReader(
                    IMCenter.getInstance().getContext().getAssets().open(JSON_FILE_NAME)));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            RLog.e(TAG, "getJson", e);
        } finally {
            try {
                if (bf != null) {
                    bf.close();
                }
            } catch (IOException e) {
                RLog.e(TAG, "getJson", e);
            }
        }
        return stringBuilder.toString();
    }

    private Map<String, String> setData(String str) {
        try {
            JSONObject jsonObj = new JSONObject(str);
            DATA.put(TAG_BASE_HEAD, jsonObj.optString(TAG_BASE_HEAD));
            DATA.put(TAG_TIME, jsonObj.optString(TAG_TIME));
            DATA.put(TAG_TXT, jsonObj.optString(TAG_TXT));
            DATA.put(TAG_SIGHT, jsonObj.optString(TAG_SIGHT));
            DATA.put(TAG_IMG, jsonObj.optString(TAG_IMG));
            DATA.put(TAG_GIF, jsonObj.optString(TAG_GIF));
            DATA.put(TAG_COMBINE, jsonObj.optString(TAG_COMBINE));
            DATA.put(TAG_MSG_COMBINE_BODY, jsonObj.optString(TAG_MSG_COMBINE_BODY));
            DATA.put(TAG_FILE, jsonObj.optString(TAG_FILE));
            DATA.put(TAG_VC, jsonObj.optString(TAG_VC));
            DATA.put(TAG_CARD, jsonObj.optString(TAG_CARD));
            DATA.put(TAG_STK, jsonObj.optString(TAG_STK));
            DATA.put(TAG_IMG_TEXT, jsonObj.optString(TAG_IMG_TEXT));
            DATA.put(TAG_LBS, jsonObj.optString(TAG_LBS));
            DATA.put(TAG_VCSUMMARY, jsonObj.optString(TAG_VCSUMMARY));
            DATA.put(TAG_RP, jsonObj.optString(TAG_RP));
            DATA.put(TAG_BASE_BOTTOM, jsonObj.optString(TAG_BASE_BOTTOM));
            return DATA;
        } catch (JSONException e) {
            e.printStackTrace();
            return DATA;
        }
    }

    private String setUserInfo(String str, Message msg) {
        String portrait = getUserPortrait(msg);
        String showUser = TextUtils.isEmpty(portrait) ? NO_USER : "";
        return str.replace(MSG_PORTRAIT, portrait)
                .replace(MSG_SHOW_USER, showUser)
                .replace(MSG_USER_NAMEM, getUserName(msg))
                .replace(MSG_SEND_TIME, getSendTime(msg));
    }

    private String getSendTime(Message msg) {
        long dateMillis = msg.getSentTime();
        Context context = IMCenter.getInstance().getContext();
        if (dateMillis <= 0) {
            return "";
        }

        String hourTime;
        if (RongDateUtils.isTime24Hour(context)) {
            hourTime = new SimpleDateFormat("H:mm", Locale.CANADA).format(dateMillis);
        } else {
            Calendar calendarTime = Calendar.getInstance();
            calendarTime.setTimeInMillis(dateMillis);
            int hour = calendarTime.get(Calendar.HOUR);
            int minute = calendarTime.get(Calendar.MINUTE);

            if (calendarTime.get(Calendar.AM_PM) == Calendar.AM) { //AM
                if (hour < 6) { //凌晨
                    if (hour == 0) {
                        hour = 12;
                    }
                    hourTime = context.getResources().getString(R.string.rc_daybreak_format);
                } else { //上午
                    hourTime = context.getResources().getString(R.string.rc_morning_format);
                }
            } else {//PM
                if (hour == 0) { //中午
                    hour = 12;
                    hourTime = context.getResources().getString(R.string.rc_noon_format);
                } else if (hour <= 5) { //下午
                    hourTime = context.getResources().getString(R.string.rc_afternoon_format);
                } else {//晚上
                    hourTime = context.getResources().getString(R.string.rc_night_format);
                }
            }
            if (minute < 10) {
                hourTime = hourTime + " " + hour + ":0" + minute;
            } else {
                hourTime = hourTime + " " + hour + ":" + minute;
            }
        }

        String format;
        if (isSameDay) {
            format = "";
        } else if (isSameYear) {
            format = "M-d ";
        } else {
            format = "yyyy-M-d ";
        }
        return new SimpleDateFormat(format, Locale.CANADA).format(dateMillis) + hourTime;
    }

    private String getUserName(Message msg) {
        UserInfo info = RongUserInfoManager.getInstance().getUserInfo(msg.getSenderUserId());
        if (info == null) return "";
        return info.getName();
    }

    private String getUserPortrait(Message msg) {
        UserInfo info = RongUserInfoManager.getInstance().getUserInfo(msg.getSenderUserId());
        if (info == null) {
            RLog.d(TAG, "getUserPortrait userInfo is null, msg:" + msg);
            return "";
        }

        Uri uri = info.getPortraitUri();
        String userId = info.getUserId();
        if (uri == null || userId == null || uri.equals(URI) && userId.equals(sendUserId)) {
            Log.d(TAG, "getUserPortrait is same uri:" + uri);
            return "";
        }
        URI = uri;
        sendUserId = userId;
        return getBase64FromUrl(uri);
    }

    private String getBase64FromUrl(Uri uri) {
        if (uri == null) return "";

        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equals("file")) {
            RLog.d(TAG, "getBase64FromUrl uri is not file, uri:" + uri.toString());
            return uri.toString();
        }

        Bitmap bitmap = null;
        try {
            bitmap = BitmapUtil.getResizedBitmap(IMCenter.getInstance().getContext(), uri, IMAGE_WIDTH, IMAGE_HEIGHT);
        } catch (IOException e) {
            RLog.e(TAG, "getBase64FromUrl", e);
        }

        if (bitmap == null) {
            RLog.e(TAG, "getBase64FromUrl bitmap is null, uri:" + uri.toString());
            return "";
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();
        return BASE64_PRE + Base64.encodeToString(data, Base64.NO_WRAP);
    }

    private String getBase64FromImageId(int id) {

        Bitmap bitmap = BitmapFactory.decodeResource(IMCenter.getInstance().getContext().getResources(), id);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();
        return BASE64_PRE + Base64.encodeToString(data, Base64.NO_WRAP);
    }

    private String getSpannable(MessageContent content) {
        Spannable spannable = RongConfigCenter.conversationConfig().getMessageSummary(IMCenter.getInstance().getContext(), content);
        if (spannable == null) return "";
        return spannable.toString();
    }

    // 用户调用该方法可传入自定义样式
    public void setStyle(String STYLE) {
        this.style = STYLE;
    }

    // 根据链接地址，获取合并转发消息下载路径
    public String getCombineFilePath(String uri) {
        return FileUtils.getCachePath(IMCenter.getInstance().getContext())
                + File.separator + COMBINE_FILE_PATH
                + File.separator + RongUtils.md5(uri) + COMBINE_FILE_SUFFIX;
    }

}
