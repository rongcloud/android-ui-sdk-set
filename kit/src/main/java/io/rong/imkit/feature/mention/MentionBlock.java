package io.rong.imkit.feature.mention;


import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class MentionBlock {
    public String userId;
    public String name;
    public boolean offset;
    public int start;
    public int end;

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            return jsonObject.putOpt("userId", userId)
                    .putOpt("name", name)
                    .putOpt("offset", offset)
                    .putOpt("start", start)
                    .putOpt("end", end);
        } catch (JSONException e) {
            return null;
        }
    }

    @NonNull
    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        try {
            return jsonObject.putOpt("userId", userId)
                    .putOpt("name", name)
                    .putOpt("offset", offset)
                    .putOpt("start", start)
                    .putOpt("end", end)
                    .toString();
        } catch (JSONException e) {
            return super.toString();
        }
    }

    MentionBlock() {}

    MentionBlock(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            userId = jsonObject.optString("userId");
            name = jsonObject.optString("name");
            offset = jsonObject.optBoolean("offset");
            start = jsonObject.optInt("start");
            end = jsonObject.optInt("end");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
