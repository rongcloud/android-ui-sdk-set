package io.rong.imkit.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import io.rong.common.rlog.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.event.actionevent.BaseMessageEvent;
import io.rong.imkit.event.actionevent.DeleteEvent;
import io.rong.imkit.event.actionevent.RecallEvent;
import io.rong.imkit.feature.destruct.DestructManager;
import io.rong.imkit.utils.AndroidConstant;
import io.rong.imkit.utils.KitStorageUtils;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.utils.RongUtils;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imkit.widget.dialog.OptionsPopupDialog;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.message.GIFMessage;
import io.rong.message.RecallNotificationMessage;
import java.io.File;
import java.lang.ref.WeakReference;

public class GIFPreviewActivity extends RongBaseNoActionbarActivity {
    TextView mCountDownView;
    TextView mFailedTxt;
    Message currentMessage;
    private static final String TAG = "GIFPreviewActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_gif_preview);
        mCountDownView = findViewById(R.id.rc_count_down);
        mFailedTxt = findViewById(R.id.rc_gif_txt);
        final ImageView gifPreview = findViewById(R.id.rc_gif_preview);
        currentMessage = getIntent().getParcelableExtra("message");
        if (currentMessage == null
                || currentMessage.getContent() == null
                || !(currentMessage.getContent() instanceof GIFMessage)) {
            finish();
            return;
        }

        gifPreview.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        GIFMessage gifMessage = (GIFMessage) currentMessage.getContent();
                        if (!gifMessage.isDestruct()) {
                            saveGif(gifMessage);
                        }
                        return false;
                    }
                });

        gifPreview.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Window window = GIFPreviewActivity.this.getWindow();
                        if (window != null) {
                            int flag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
                            window.setFlags(flag, flag);
                        }
                        finish();
                    }
                });

        GIFMessage gifMessage = (GIFMessage) currentMessage.getContent();
        if (gifMessage.isDestruct()
                && currentMessage.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
            DestructManager.getInstance()
                    .addListener(
                            currentMessage.getUId(),
                            new DestructListener(mCountDownView, currentMessage.getUId()),
                            TAG);
        }

        if (gifMessage.getLocalUri() == null) {
            IMCenter.getInstance()
                    .downloadMediaMessage(
                            currentMessage,
                            new IRongCallback.IDownloadMediaMessageCallback() {
                                @Override
                                public void onSuccess(Message message) {
                                    loadGif(gifPreview, (GIFMessage) message.getContent());
                                }

                                @Override
                                public void onProgress(Message message, int progress) {
                                    // do nothing
                                }

                                @Override
                                public void onError(Message message, RongIMClient.ErrorCode code) {
                                    // do nothing
                                }

                                @Override
                                public void onCanceled(Message message) {
                                    // do nothing
                                }
                            });
        } else {
            loadGif(gifPreview, gifMessage);
        }

        IMCenter.getInstance().addMessageEventListener(mBaseMessageEvent);
        IMCenter.getInstance().addOnRecallMessageListener(mRecallMessageListener);
    }

    private void loadGif(ImageView gifPreview, GIFMessage gifMessage) {
        if (RongUtils.isDestroy(GIFPreviewActivity.this)) {
            return;
        }
        Glide.with(this)
                .asGif()
                .error(R.drawable.rc_received_thumb_image_broken)
                .addListener(
                        new RequestListener<GifDrawable>() {
                            @Override
                            public boolean onLoadFailed(
                                    @Nullable GlideException e,
                                    Object model,
                                    Target<GifDrawable> target,
                                    boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(
                                    GifDrawable resource,
                                    Object model,
                                    Target<GifDrawable> target,
                                    DataSource dataSource,
                                    boolean isFirstResource) {
                                if (currentMessage.getContent().isDestruct()
                                        && currentMessage.getMessageDirection()
                                                == Message.MessageDirection.RECEIVE
                                        && currentMessage.getReadTime() <= 0
                                        && !TextUtils.isEmpty(currentMessage.getUId())) {
                                    DestructManager.getInstance().startDestruct(currentMessage);
                                    // todo
                                    // EventBus.getDefault().post(new
                                    // Event.changeDestructionReadTimeEvent(currentMessage));
                                }
                                return false;
                            }
                        })
                .load(gifMessage.getLocalUri().getPath())
                .into(gifPreview);
    }

    private void saveGif(GIFMessage message) {
        String path = message.getLocalUri().getPath();
        final File file = new File(path);
        if (!file.exists()) {
            return;
        }
        String[] items = new String[] {getString(R.string.rc_save_picture)};
        OptionsPopupDialog.newInstance(GIFPreviewActivity.this, items)
                .setOptionsPopupDialogListener(
                        new OptionsPopupDialog.OnOptionsItemClickedListener() {
                            @Override
                            public void onOptionsItemClicked(int which) {
                                if (which == 0) {
                                    String[] permissions = {
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    };
                                    if (Build.VERSION.SDK_INT < AndroidConstant.ANDROID_TIRAMISU
                                            && !PermissionCheckUtil.requestPermissions(
                                                    GIFPreviewActivity.this, permissions)) {
                                        return;
                                    }

                                    String text =
                                            GIFPreviewActivity.this.getString(
                                                    R.string.rc_save_picture_at);
                                    if (file.exists()) {
                                        KitStorageUtils.saveMediaToPublicDir(
                                                GIFPreviewActivity.this,
                                                file,
                                                KitStorageUtils.MediaType.IMAGE);

                                    } else {
                                        text = getString(R.string.rc_src_file_not_found);
                                    }

                                    ToastUtils.show(
                                            GIFPreviewActivity.this, text, Toast.LENGTH_SHORT);
                                }
                            }
                        })
                .show();
    }

    private static class DestructListener implements RongIMClient.DestructCountDownTimerListener {
        private WeakReference<TextView> mCountDownView;
        private String mMessageId;

        public DestructListener(TextView pCountDownView, String pMessageId) {
            mCountDownView = new WeakReference<>(pCountDownView);
            mMessageId = pMessageId;
        }

        @Override
        public void onTick(long millisUntilFinished, String pMessageId) {
            if (mMessageId.equals(pMessageId)) {
                TextView countDownView = mCountDownView.get();
                if (countDownView != null) {
                    countDownView.setVisibility(View.VISIBLE);
                    countDownView.setText(String.valueOf(Math.max(millisUntilFinished, 1)));
                }
            }
        }

        @Override
        public void onStop(String messageId) {
            if (mMessageId.equals(messageId)) {
                TextView countDownView = mCountDownView.get();
                if (countDownView != null) {
                    countDownView.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IMCenter.getInstance().removeMessageEventListener(mBaseMessageEvent);
        IMCenter.getInstance().removeOnRecallMessageListener(mRecallMessageListener);
    }

    @Override
    public void finish() {
        super.finish();
        // 全屏Activity在finish后回到非全屏，会造成页面重绘闪动问题（典型现象是RecyclerView向下滑动一点距离）
        // finish后清除全屏标志位，避免此问题
        if (getWindow() != null) {
            int flag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
            getWindow().setFlags(flag, flag);
        }
    }

    BaseMessageEvent mBaseMessageEvent =
            new BaseMessageEvent() {
                @Override
                public void onRecallEvent(RecallEvent event) {
                    if (currentMessage == null) {
                        return;
                    }
                    int messageId = currentMessage.getMessageId();
                    if (messageId == event.getMessageId()) {
                        new AlertDialog.Builder(
                                        GIFPreviewActivity.this,
                                        AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                                .setMessage(getString(R.string.rc_recall_success))
                                .setPositiveButton(
                                        getString(R.string.rc_dialog_ok),
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                finish();
                                            }
                                        })
                                .setCancelable(false)
                                .show();
                    }
                }

                @Override
                public void onDeleteMessage(DeleteEvent event) {
                    RLog.d(TAG, "MessageDeleteEvent");
                    if (event.getMessageIds() != null && currentMessage != null) {
                        for (int messageId : event.getMessageIds()) {
                            if (messageId == currentMessage.getMessageId()) {
                                finish();
                                break;
                            }
                        }
                    }
                }
            };
    RongIMClient.OnRecallMessageListener mRecallMessageListener =
            new RongIMClient.OnRecallMessageListener() {
                @Override
                public boolean onMessageRecalled(
                        Message message, RecallNotificationMessage recallNotificationMessage) {
                    if (currentMessage == null) {
                        return false;
                    }
                    int messageId = currentMessage.getMessageId();
                    if (messageId == message.getMessageId()) {
                        new AlertDialog.Builder(
                                        GIFPreviewActivity.this,
                                        AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                                .setMessage(getString(R.string.rc_recall_success))
                                .setPositiveButton(
                                        getString(R.string.rc_dialog_ok),
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                finish();
                                            }
                                        })
                                .setCancelable(false)
                                .show();
                    }
                    return false;
                }
            };
}
