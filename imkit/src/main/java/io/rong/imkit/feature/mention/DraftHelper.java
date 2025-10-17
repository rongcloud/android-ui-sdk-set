package io.rong.imkit.feature.mention;

import android.text.TextUtils;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;

/**
 * Created by luoyanlong on 2018/11/23. 保存草稿时如果有@信息，把MentionBlock序列化保存起来，
 * 之后可通过反序列化添加到RongMentionManager中
 */
public class DraftHelper {

    private final EditText editText;

    public DraftHelper(@NonNull EditText editText) {
        this.editText = editText;
    }

    public String getMentionBlocks() {
        MentionInstance mentionInstance =
                RongMentionManager.getInstance().obtainMentionInstance(editText);
        if (mentionInstance == null
                || mentionInstance.mentionBlocks == null
                || mentionInstance.mentionBlocks.isEmpty()) {
            return "";
        }
        JSONArray jsonArray = new JSONArray();
        for (MentionBlock mentionBlock : mentionInstance.mentionBlocks) {
            jsonArray.put(mentionBlock.toJson());
        }
        return jsonArray.toString();
    }

    public void addMentionBlocks(String mentionInfo) {
        MentionInstance mentionInstance =
                RongMentionManager.getInstance().obtainMentionInstance(editText);
        if (mentionInstance == null || mentionInstance.mentionBlocks == null) {
            return;
        }

        List<MentionBlock> mentionBlocks = parseMentionBlocks(mentionInfo);
        if (mentionBlocks == null || mentionBlocks.isEmpty()) {
            return;
        }
        for (MentionBlock mentionBlock : mentionBlocks) {
            if (mentionBlock != null && !TextUtils.isEmpty(mentionBlock.userId)) {
                mentionInstance.mentionBlocks.add(mentionBlock);
            }
        }
    }

    @Nullable
    private List<MentionBlock> parseMentionBlocks(String mentionInfo) {
        if (TextUtils.isEmpty(mentionInfo)) {
            return Collections.emptyList();
        }

        try {
            JSONArray jsonArray = new JSONArray(mentionInfo);
            List<MentionBlock> list = new ArrayList<>(jsonArray.length());
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(new MentionBlock(jsonArray.getString(i)));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
