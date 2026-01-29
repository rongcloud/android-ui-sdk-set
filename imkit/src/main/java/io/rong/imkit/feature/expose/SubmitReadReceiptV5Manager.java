package io.rong.imkit.feature.expose;

import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.ConversationIdentifier;
import java.util.List;

/** 批量提交已读回执V5管理器 */
public class SubmitReadReceiptV5Manager extends ExposeBatchSubmitManager<String> {
    private ConversationIdentifier mId;

    /** 构造函数 */
    public SubmitReadReceiptV5Manager() {
        super();
    }

    public void bindConversation(ConversationIdentifier id) {
        this.mId = id;
    }

    @Override
    public void addSubmitTask(String item) {
        if (mId == null) {
            return;
        }
        super.addSubmitTask(item);
    }

    @Override
    void onBatchSubmit(List<String> items, BatchResultCallback callback) {
        if (mId == null) {
            callback.onResult(IRongCoreEnum.CoreErrorCode.SUCCESS, false);
            return;
        }
        RongCoreClient.getInstance()
                .sendReadReceiptResponseV5(
                        mId,
                        items,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                callback.onResult(IRongCoreEnum.CoreErrorCode.SUCCESS, false);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode code) {
                                if (code
                                        == IRongCoreEnum.CoreErrorCode
                                                .MESSAGE_READ_RECEIPT_NOT_SUPPORT) {
                                    callback.onResult(code, false);
                                } else {
                                    callback.onResult(code, true);
                                }
                            }
                        });
    }
}
