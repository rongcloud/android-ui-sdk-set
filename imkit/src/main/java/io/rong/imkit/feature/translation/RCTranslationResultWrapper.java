package io.rong.imkit.feature.translation;

import android.os.Parcel;
import android.os.Parcelable;

/** @author gusd */
public class RCTranslationResultWrapper implements Parcelable {

    /** 消息 ID */
    private int messageId;
    /** 原文本 */
    private String srcText;

    /** 翻译文本 */
    private String translatedText;

    /** 原语言类型 */
    private String srcLanguage;

    /** 目标语言类型 */
    private String targetLanguage;

    public RCTranslationResultWrapper(
            int messageId,
            String srcText,
            String translatedText,
            String srcLanguage,
            String targetLanguage) {
        this.messageId = messageId;
        this.srcText = srcText;
        this.translatedText = translatedText;
        this.srcLanguage = srcLanguage;
        this.targetLanguage = targetLanguage;
    }

    public int getMessageId() {
        return messageId;
    }

    public String getSrcText() {
        return srcText;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public String getSrcLanguage() {
        return srcLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.messageId);
        dest.writeString(this.srcText);
        dest.writeString(this.translatedText);
        dest.writeString(this.srcLanguage);
        dest.writeString(this.targetLanguage);
    }

    public void readFromParcel(Parcel source) {
        this.messageId = source.readInt();
        this.srcText = source.readString();
        this.translatedText = source.readString();
        this.srcLanguage = source.readString();
        this.targetLanguage = source.readString();
    }

    public RCTranslationResultWrapper() {}

    protected RCTranslationResultWrapper(Parcel in) {
        this.messageId = in.readInt();
        this.srcText = in.readString();
        this.translatedText = in.readString();
        this.srcLanguage = in.readString();
        this.targetLanguage = in.readString();
    }

    public static final Creator<RCTranslationResultWrapper> CREATOR =
            new Creator<RCTranslationResultWrapper>() {
                @Override
                public RCTranslationResultWrapper createFromParcel(Parcel source) {
                    return new RCTranslationResultWrapper(source);
                }

                @Override
                public RCTranslationResultWrapper[] newArray(int size) {
                    return new RCTranslationResultWrapper[size];
                }
            };
}
