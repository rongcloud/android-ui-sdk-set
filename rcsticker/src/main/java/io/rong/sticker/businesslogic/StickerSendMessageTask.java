package io.rong.sticker.businesslogic;

import java.util.Locale;

import io.rong.imkit.IMCenter;
import io.rong.sticker.message.StickerMessage;
import io.rong.sticker.model.Sticker;
import io.rong.imkit.RongIM;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;

/**
 * Created by luoyanlong on 2018/08/24.
 * 发送表情消息
 */
public class StickerSendMessageTask {

    private static String sTargetId;
    private static Conversation.ConversationType sConversationType;
    private static final String FORMAT = "[%s]"; // 推送格式

    public static void config(String targetId, Conversation.ConversationType conversationType) {
        sTargetId = targetId;
        sConversationType = conversationType;
    }

    public static void sendMessage(Sticker sticker) {
        StickerMessage stickerMessage = StickerMessage.obtain(sticker);
        Message message = Message.obtain(sTargetId, sConversationType, stickerMessage);
        String pushContent = String.format(Locale.getDefault(), FORMAT, stickerMessage.getDigest());
        IMCenter.getInstance().sendMessage(message, null, null, (IRongCallback.ISendMessageCallback) null);
    }

}
