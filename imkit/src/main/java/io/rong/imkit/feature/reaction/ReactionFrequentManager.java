package io.rong.imkit.feature.reaction;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 回应表情使用频次管理器。
 *
 * @since 5.42.0
 */
public class ReactionFrequentManager {
    private static final String SP_NAME = "RongReactionFrequent";
    private static final String KEY_FREQUENT_LIST = "frequent_list";
    private static final int MAX_SIZE = 20;
    private static final int DEFAULT_DISPLAY_COUNT = 14;

    private final SharedPreferences sp;

    public ReactionFrequentManager(Context context) {
        sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    public void recordUsage(String reactionId) {
        List<FrequentItem> list = loadList();
        FrequentItem target = null;
        for (FrequentItem item : list) {
            if (item.reactionId.equals(reactionId)) {
                target = item;
                break;
            }
        }
        if (target != null) {
            target.count++;
            target.lastUsedTime = System.currentTimeMillis();
        } else {
            sortByUsage(list);
            if (list.size() >= MAX_SIZE) {
                list.remove(list.size() - 1);
            }
            list.add(new FrequentItem(reactionId, 1, System.currentTimeMillis()));
        }
        sortByUsage(list);
        saveList(list);
    }

    public List<String> getFrequentReactionIds(int maxCount) {
        List<FrequentItem> list = loadList();
        sortByUsage(list);
        List<String> result = new ArrayList<>();
        int limit = Math.min(maxCount, list.size());
        for (int i = 0; i < limit; i++) {
            result.add(list.get(i).reactionId);
        }
        return result;
    }

    public List<String> getFrequentReactionIds() {
        return getFrequentReactionIds(DEFAULT_DISPLAY_COUNT);
    }

    private List<FrequentItem> loadList() {
        String json = sp.getString(KEY_FREQUENT_LIST, null);
        List<FrequentItem> list = new ArrayList<>();
        if (json == null) {
            return list;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(
                        new FrequentItem(
                                obj.getString("id"), obj.getInt("count"), obj.getLong("time")));
            }
        } catch (JSONException e) {
            // ignore
        }
        return list;
    }

    private void saveList(List<FrequentItem> list) {
        JSONArray array = new JSONArray();
        for (FrequentItem item : list) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", item.reactionId);
                obj.put("count", item.count);
                obj.put("time", item.lastUsedTime);
                array.put(obj);
            } catch (JSONException e) {
                // ignore
            }
        }
        sp.edit().putString(KEY_FREQUENT_LIST, array.toString()).apply();
    }

    private void sortByUsage(List<FrequentItem> list) {
        Collections.sort(
                list,
                (a, b) -> {
                    if (a.count != b.count) {
                        return b.count - a.count;
                    }
                    return Long.compare(b.lastUsedTime, a.lastUsedTime);
                });
    }

    static class FrequentItem {
        String reactionId;
        int count;
        long lastUsedTime;

        FrequentItem(String id, int count, long time) {
            this.reactionId = id;
            this.count = count;
            this.lastUsedTime = time;
        }
    }
}
