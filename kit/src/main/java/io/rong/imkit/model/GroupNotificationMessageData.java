package io.rong.imkit.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by AMing on 16/2/18.
 * Company RongCloud
 */
public class GroupNotificationMessageData {


    /**
     * timestamp : 1456366634327
     * targetUserDisplayNames : ["android"]
     * targetUserIds : ["Cz3bcYl2K"]
     * operatorNickname : 赵哈哈
     *
     * 这个是Data里边的Json数据结构;
     * {"operatorNickname":"田奎","targetUserIds":["wGPkc0VpO"],"targetUserDisplayNames":["郝腾飞"],"timestamp":1477042403674,
     * "data":{"operatorNickname":"田奎","targetUserIds":["wGPkc0VpO"],"targetUserDisplayNames":["郝腾飞"],"timestamp":1477042403674}}
     */
    private long timestamp;
    private String operatorNickname;
    private String targetGroupName;
    private List<String> targetUserDisplayNames = new ArrayList<>();
    private List<String> targetUserIds = new ArrayList<>();
    private String oldCreatorId;
    private String oldCreatorName;
    private String newCreatorId;
    private String newCreatorName;

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setOperatorNickname(String operatorNickname) {
        this.operatorNickname = operatorNickname;
    }

    public void setTargetUserDisplayNames(List<String> targetUserDisplayNames) {
        this.targetUserDisplayNames = targetUserDisplayNames;
    }

    public void setTargetUserIds(List<String> targetUserIds) {
        this.targetUserIds = targetUserIds;
    }

    public void setTargetGroupName(String targetGroupName) {
        this.targetGroupName = targetGroupName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getOperatorNickname() {
        return operatorNickname;
    }

    public String getTargetGroupName() {
        return targetGroupName;
    }

    public List<String> getTargetUserDisplayNames() {
        return targetUserDisplayNames;
    }

    public List<String> getTargetUserIds() {
        return targetUserIds;
    }

    public String getOldCreatorId() {
        return oldCreatorId;
    }

    public void setOldCreatorId(String oldCreatorId) {
        this.oldCreatorId = oldCreatorId;
    }

    public String getOldCreatorName() {
        return oldCreatorName;
    }

    public void setOldCreatorName(String oldCreatorName) {
        this.oldCreatorName = oldCreatorName;
    }

    public String getNewCreatorId() {
        return newCreatorId;
    }

    public void setNewCreatorId(String newCreatorId) {
        this.newCreatorId = newCreatorId;
    }

    public String getNewCreatorName() {
        return newCreatorName;
    }

    public void setNewCreatorName(String newCreatorName) {
        this.newCreatorName = newCreatorName;
    }


}
