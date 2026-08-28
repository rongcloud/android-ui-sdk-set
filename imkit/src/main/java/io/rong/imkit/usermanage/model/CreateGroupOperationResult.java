package io.rong.imkit.usermanage.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import io.rong.imlib.IRongCoreEnum.CoreErrorCode;
import io.rong.imlib.model.CreateGroupErrorInfo;

/** IMKit 创建群组操作结果。 @since 5.46.0 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class CreateGroupOperationResult {

    private final boolean success;
    private final CoreErrorCode code;
    private final CreateGroupErrorInfo errorInfo;

    private CreateGroupOperationResult(
            boolean success,
            @NonNull CoreErrorCode code,
            @Nullable CreateGroupErrorInfo errorInfo) {
        this.success = success;
        this.code = code;
        this.errorInfo = errorInfo;
    }

    /**
     * @since 5.46.0
     */
    public static CreateGroupOperationResult success(@NonNull CoreErrorCode processCode) {
        return new CreateGroupOperationResult(true, processCode, null);
    }

    /**
     * @since 5.46.0
     */
    public static CreateGroupOperationResult error(
            @NonNull CoreErrorCode errorCode, @Nullable CreateGroupErrorInfo errorInfo) {
        return new CreateGroupOperationResult(false, errorCode, errorInfo);
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
    public CreateGroupErrorInfo getErrorInfo() {
        return errorInfo;
    }
}
