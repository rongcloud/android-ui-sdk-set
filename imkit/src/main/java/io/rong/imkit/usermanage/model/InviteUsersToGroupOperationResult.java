package io.rong.imkit.usermanage.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import io.rong.imlib.IRongCoreEnum.CoreErrorCode;
import io.rong.imlib.model.InviteUsersToGroupErrorInfo;

/** IMKit 邀请用户入群操作结果。 @since 5.46.0 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class InviteUsersToGroupOperationResult {

    private final boolean success;
    private final CoreErrorCode code;
    private final InviteUsersToGroupErrorInfo errorInfo;

    private InviteUsersToGroupOperationResult(
            boolean success,
            @NonNull CoreErrorCode code,
            @Nullable InviteUsersToGroupErrorInfo errorInfo) {
        this.success = success;
        this.code = code;
        this.errorInfo = errorInfo;
    }

    /**
     * @since 5.46.0
     */
    public static InviteUsersToGroupOperationResult success(@NonNull CoreErrorCode processCode) {
        return new InviteUsersToGroupOperationResult(true, processCode, null);
    }

    /**
     * @since 5.46.0
     */
    public static InviteUsersToGroupOperationResult error(
            @NonNull CoreErrorCode errorCode, @Nullable InviteUsersToGroupErrorInfo errorInfo) {
        return new InviteUsersToGroupOperationResult(false, errorCode, errorInfo);
    }

    /**
     * @since 5.46.0
     */
    public boolean isSuccess() {
        return success;
    }

    /** 返回成功时的流程码或失败时的错误码。 @since 5.46.0 */
    @NonNull
    public CoreErrorCode getCode() {
        return code;
    }

    /**
     * @since 5.46.0
     */
    @Nullable
    public InviteUsersToGroupErrorInfo getErrorInfo() {
        return errorInfo;
    }
}
