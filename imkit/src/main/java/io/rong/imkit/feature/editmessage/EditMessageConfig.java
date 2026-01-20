package io.rong.imkit.feature.editmessage;

import android.text.TextUtils;
import io.rong.imkit.feature.mention.MentionBlock;
import java.util.List;

public class EditMessageConfig {
    // 消息UID
    public String uid;
    // 消息sentTime
    public long sentTime;
    // 编辑的消息内容
    public String content;
    // 编辑的消息的引用内容
    public String referContent;
    // @内容
    public List<MentionBlock> mentionBlocks;

    public EditMessageConfig() {}

    public EditMessageConfig(
            String uid, String content, String referContent, List<MentionBlock> mentionBlocks) {
        this.uid = uid;
        this.content = content;
        this.referContent = referContent;
        this.mentionBlocks = mentionBlocks;
    }

    public static boolean isInvalid(EditMessageConfig config) {
        return config == null || TextUtils.isEmpty(config.uid) || TextUtils.isEmpty(config.content);
    }
}
