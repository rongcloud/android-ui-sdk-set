package io.rong.imkit.handler;

import android.text.TextUtils;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.usermanage.interfaces.OnPagedDataLoader;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.GetMessageReactionUsersParam;
import io.rong.imlib.model.MessageReactionUser;
import io.rong.imlib.model.PagingQueryResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 消息回应用户列表分页处理器。
 *
 * <p>使用完毕后需要调用 {@link #stop()}。
 *
 * @since 5.42.0
 */
public class ReactionUsersPagedHandler extends MultiDataHandler implements OnPagedDataLoader {

    public static final DataKey<UserPageResult> KEY_GET_REACTION_USERS =
            DataKey.obtain("KEY_GET_REACTION_USERS", UserPageResult.class);

    public static final DataKey<Boolean> KEY_LOAD_MORE =
            DataKey.obtain("KEY_REACTION_USERS_LOAD_MORE", Boolean.class);

    private final int pageSize;
    private String messageUId;
    private String reactionId;
    private String nextPageToken;
    private volatile boolean isLoading;
    private int requestSeq;
    private int totalCount;
    private int loadedCount;

    public ReactionUsersPagedHandler() {
        this(50);
    }

    public ReactionUsersPagedHandler(int pageSize) {
        this.pageSize = pageSize;
    }

    public void query(String messageUId, String reactionId) {
        this.messageUId = messageUId;
        this.reactionId = reactionId;
        this.nextPageToken = null;
        this.totalCount = 0;
        this.loadedCount = 0;
        this.requestSeq++;
        this.isLoading = false;
        loadPage(null, true, requestSeq);
    }

    private void loadPage(String pageToken, boolean firstPage, int seq) {
        if (isLoading || TextUtils.isEmpty(messageUId) || TextUtils.isEmpty(reactionId)) {
            return;
        }
        isLoading = true;
        GetMessageReactionUsersParam param =
                new GetMessageReactionUsersParam(messageUId, reactionId, pageSize);
        param.setPageToken(pageToken);
        RongCoreClient.getInstance()
                .getMessageReactionUsers(
                        param,
                        new IRongCoreCallback.PageResultCallback<MessageReactionUser>() {
                            @Override
                            public void onSuccess(PagingQueryResult<MessageReactionUser> result) {
                                if (seq != requestSeq) {
                                    return;
                                }
                                isLoading = false;
                                List<MessageReactionUser> users =
                                        result == null ? null : result.getData();
                                nextPageToken = result == null ? null : result.getPageToken();
                                int pageUserCount = users == null ? 0 : users.size();
                                loadedCount =
                                        firstPage ? pageUserCount : loadedCount + pageUserCount;
                                totalCount = result == null ? loadedCount : result.getTotalCount();
                                notifyDataChange(
                                        KEY_GET_REACTION_USERS,
                                        new UserPageResult(
                                                messageUId,
                                                reactionId,
                                                users,
                                                firstPage,
                                                totalCount,
                                                loadedCount,
                                                nextPageToken));
                                notifyDataChange(KEY_LOAD_MORE, hasNext());
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                if (seq != requestSeq) {
                                    return;
                                }
                                isLoading = false;
                                notifyDataError(KEY_GET_REACTION_USERS, e);
                                notifyDataChange(KEY_LOAD_MORE, false);
                            }
                        });
    }

    @Override
    public void loadNext(OnDataChangeListener<Boolean> listener) {
        if (listener != null) {
            replaceDataChangeListener(KEY_LOAD_MORE, listener);
        }
        if (!hasNext()) {
            notifyDataChange(KEY_LOAD_MORE, false);
            return;
        }
        loadPage(nextPageToken, false, requestSeq);
    }

    @Override
    public boolean hasNext() {
        return !TextUtils.isEmpty(nextPageToken) && (totalCount <= 0 || loadedCount < totalCount);
    }

    public boolean isLoading() {
        return isLoading;
    }

    public static class UserPageResult {
        private final String messageUId;
        private final String reactionId;
        private final List<MessageReactionUser> users;
        private final boolean firstPage;
        private final int totalCount;
        private final int loadedCount;
        private final String nextPageToken;

        UserPageResult(
                String messageUId,
                String reactionId,
                List<MessageReactionUser> users,
                boolean firstPage,
                int totalCount,
                int loadedCount,
                String nextPageToken) {
            this.messageUId = messageUId;
            this.reactionId = reactionId;
            this.users = users == null ? Collections.emptyList() : new ArrayList<>(users);
            this.firstPage = firstPage;
            this.totalCount = totalCount;
            this.loadedCount = loadedCount;
            this.nextPageToken = nextPageToken;
        }

        public String getMessageUId() {
            return messageUId;
        }

        public String getReactionId() {
            return reactionId;
        }

        public List<MessageReactionUser> getUsers() {
            return users;
        }

        public boolean isFirstPage() {
            return firstPage;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getLoadedCount() {
            return loadedCount;
        }

        public String getNextPageToken() {
            return nextPageToken;
        }
    }
}
