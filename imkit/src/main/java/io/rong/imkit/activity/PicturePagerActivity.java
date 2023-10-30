package io.rong.imkit.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import io.rong.common.FileUtils;
import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.event.actionevent.BaseMessageEvent;
import io.rong.imkit.event.actionevent.DeleteEvent;
import io.rong.imkit.event.actionevent.RecallEvent;
import io.rong.imkit.feature.destruct.DestructManager;
import io.rong.imkit.picture.widget.longimage.SubsamplingScaleImageView;
import io.rong.imkit.picture.widget.longimage.Utils;
import io.rong.imkit.utils.AndroidConstant;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imkit.utils.GlideUtils;
import io.rong.imkit.utils.KitStorageUtils;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imkit.widget.dialog.OptionsPopupDialog;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCommonDefine;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongCoreClientImpl;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.filetransfer.upload.MediaUploadAuthorInfo;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.message.ImageMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PicturePagerActivity extends RongBaseNoActionbarActivity
        implements View.OnLongClickListener {
    private static final String TAG = "PicturePagerActivity";
    private static final int IMAGE_MESSAGE_COUNT = 10; // 每次获取的图片消息数量。
    private static final int LOAD_PICTURE_TIMEOUT = 30 * 1000; //  最小图片加载时间
    protected ViewPager2 mViewPager;
    protected ImageMessage mCurrentImageMessage;
    protected Message mMessage;
    protected Conversation.ConversationType mConversationType;
    protected int mCurrentMessageId;
    protected int currentSelectMessageId;
    protected String mTargetId = null;
    protected ImageAdapter mImageAdapter;
    protected boolean isFirstTime = false;

    protected ViewPager2.OnPageChangeCallback mPageChangeListener =
            new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    if (null == mImageAdapter) {
                        return;
                    }
                    if (mImageAdapter.getItemCount() <= 0) {
                        return;
                    }
                    // position 越界直接返回
                    if (position >= mImageAdapter.getItemCount()) {
                        return;
                    }
                    ImageInfo imageInfo = mImageAdapter.getItem(position);
                    if (null == imageInfo) {
                        return;
                    }
                    Message message = imageInfo.getMessage();
                    if (null == message) {
                        return;
                    }
                    int msgId = message.getMessageId();
                    if (position == (mImageAdapter.getItemCount() - 1)) {
                        getConversationImageUris(
                                msgId, RongCommonDefine.GetMessageDirection.BEHIND);
                    } else if (position == 0) {
                        getConversationImageUris(msgId, RongCommonDefine.GetMessageDirection.FRONT);
                    }
                    currentSelectMessageId = msgId;
                }
            };
    RongIMClient.OnRecallMessageListener mOnRecallMessageListener =
            new RongIMClient.OnRecallMessageListener() {
                @Override
                public boolean onMessageRecalled(
                        Message message, RecallNotificationMessage recallNotificationMessage) {
                    if (currentSelectMessageId == message.getMessageId()) {
                        new AlertDialog.Builder(
                                        PicturePagerActivity.this,
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
                    } else {
                        mImageAdapter.removeRecallItem(message.getMessageId());
                        if (mImageAdapter.getItemCount() == 0) {
                            finish();
                        }
                    }
                    return false;
                }
            };
    BaseMessageEvent mBaseMessageEvent =
            new BaseMessageEvent() {
                @Override
                public void onDeleteMessage(DeleteEvent event) {
                    RLog.d(TAG, "MessageDeleteEvent");
                    if (event.getMessageIds() != null) {
                        for (int messageId : event.getMessageIds()) {
                            mImageAdapter.removeRecallItem(messageId);
                        }
                        mImageAdapter.notifyDataSetChanged();
                        if (mImageAdapter.getItemCount() == 0) {
                            finish();
                        }
                    }
                }

                @Override
                public void onRecallEvent(RecallEvent event) {
                    if (currentSelectMessageId == event.getMessageId()) {
                        new AlertDialog.Builder(
                                        PicturePagerActivity.this,
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
                    } else {
                        mImageAdapter.removeRecallItem(event.getMessageId());
                        mImageAdapter.notifyDataSetChanged();
                        if (mImageAdapter.getItemCount() == 0) {
                            finish();
                        }
                    }
                }
            };

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mImageAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_fr_photo);
        Message currentMessage = getIntent().getParcelableExtra("message");

        mMessage = currentMessage;
        if (currentMessage.getContent() instanceof ReferenceMessage) {
            ReferenceMessage referenceMessage = (ReferenceMessage) currentMessage.getContent();
            mCurrentImageMessage = (ImageMessage) referenceMessage.getReferenceContent();
        } else {
            mCurrentImageMessage = (ImageMessage) currentMessage.getContent();
        }
        mConversationType = currentMessage.getConversationType();
        mCurrentMessageId = currentMessage.getMessageId();
        currentSelectMessageId = mCurrentMessageId;
        mTargetId = currentMessage.getTargetId();

        mViewPager = findViewById(R.id.viewpager);
        mViewPager.registerOnPageChangeCallback(mPageChangeListener);
        mImageAdapter = new ImageAdapter();
        isFirstTime = true;
        if (!(mMessage.getContent().isDestruct()
                || mMessage.getContent() instanceof ReferenceMessage
                || Conversation.ConversationType.ULTRA_GROUP.equals(
                        mMessage.getConversationType()))) {
            getConversationImageUris(
                    mCurrentMessageId,
                    RongCommonDefine.GetMessageDirection.FRONT); // 获取当前点开图片之前的图片消息。
            getConversationImageUris(
                    mCurrentMessageId, RongCommonDefine.GetMessageDirection.BEHIND);
        } else {
            // 阅后即焚和引用消息只显示1张照片
            ArrayList<ImageInfo> lists = new ArrayList<>();
            lists.add(
                    new ImageInfo(
                            mMessage,
                            mCurrentImageMessage.getThumUri(),
                            !FileUtils.isFileExistsWithUri(this, mCurrentImageMessage.getLocalUri())
                                    ? mCurrentImageMessage.getRemoteUri()
                                    : mCurrentImageMessage.getLocalUri()));
            mImageAdapter.addData(lists, true);
        }
        mViewPager.setAdapter(mImageAdapter);
        IMCenter.getInstance().addMessageEventListener(mBaseMessageEvent);
        IMCenter.getInstance().addOnRecallMessageListener(mOnRecallMessageListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IMCenter.getInstance().removeOnRecallMessageListener(mOnRecallMessageListener);
        IMCenter.getInstance().removeMessageEventListener(mBaseMessageEvent);
    }

    @Override
    public void finish() {
        super.finish();
        // 全屏Activity在finish后回到非全屏，会造成页面重绘闪动问题（典型现象是RecyclerView向下滑动一点距离）
        // finish后清除全屏标志位，避免此问题
        int flagForceNotFullscreen = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
        getWindow().setFlags(flagForceNotFullscreen, flagForceNotFullscreen);
    }

    private void getConversationImageUris(
            int messageId, final RongCommonDefine.GetMessageDirection direction) {
        if (mConversationType != null && !TextUtils.isEmpty(mTargetId)) {
            RongIMClient.getInstance()
                    .getHistoryMessages(
                            mConversationType,
                            mTargetId,
                            "RC:ImgMsg",
                            messageId,
                            IMAGE_MESSAGE_COUNT,
                            direction,
                            new RongIMClient.ResultCallback<List<Message>>() {
                                @Override
                                public void onSuccess(List<Message> messages) {
                                    int i;
                                    ArrayList<ImageInfo> lists = new ArrayList<>();
                                    if (messages != null) {
                                        if (direction.equals(
                                                RongCommonDefine.GetMessageDirection.FRONT)) {
                                            Collections.reverse(messages);
                                        }
                                        for (i = 0; i < messages.size(); i++) {
                                            Message message = messages.get(i);
                                            if (message.getContent() instanceof ImageMessage
                                                    && !message.getContent().isDestruct()) {
                                                ImageMessage imageMessage =
                                                        (ImageMessage) message.getContent();
                                                Uri largeImageUri =
                                                        !FileUtils.isFileExistsWithUri(
                                                                        PicturePagerActivity.this,
                                                                        mCurrentImageMessage
                                                                                .getLocalUri())
                                                                ? imageMessage.getRemoteUri()
                                                                : imageMessage.getLocalUri();

                                                if (imageMessage.getThumUri() != null
                                                        && largeImageUri != null) {
                                                    lists.add(
                                                            new ImageInfo(
                                                                    message,
                                                                    imageMessage.getThumUri(),
                                                                    largeImageUri));
                                                }
                                            }
                                        }
                                    }
                                    if (direction.equals(
                                            RongCommonDefine.GetMessageDirection.FRONT)) {
                                        if (isFirstTime) {
                                            lists.add(
                                                    new ImageInfo(
                                                            mMessage,
                                                            mCurrentImageMessage.getThumUri(),
                                                            !FileUtils.isFileExistsWithUri(
                                                                            PicturePagerActivity
                                                                                    .this,
                                                                            mCurrentImageMessage
                                                                                    .getLocalUri())
                                                                    ? mCurrentImageMessage
                                                                            .getRemoteUri()
                                                                    : mCurrentImageMessage
                                                                            .getLocalUri()));
                                        }
                                        mImageAdapter.addData(lists, true);
                                        if (isFirstTime) {
                                            int index =
                                                    mImageAdapter.getIndexByMessageId(
                                                            mMessage.getMessageId());
                                            if (index != -1) {
                                                mViewPager.setCurrentItem(index, false);
                                            }
                                            isFirstTime = false;
                                        }
                                    } else if (lists.size() > 0) {
                                        mImageAdapter.addData(lists, false);
                                    }
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode e) {
                                    // do nothing
                                }
                            });
        }
    }

    @Override
    public void onSaveInstanceState(
            @NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {

        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public boolean onLongClick(View v) {
        if (mCurrentImageMessage.isDestruct()) {
            return false;
        }

        ImageInfo imageInfo = mImageAdapter.getItem(mViewPager.getCurrentItem());
        if (imageInfo != null && imageInfo.isDownload()) {
            Uri thumbUri = imageInfo.getThumbUri();
            final Uri largeImageUri = imageInfo.getLargeImageUri();
            if (onPictureLongClick(v, thumbUri, largeImageUri)) {
                return true;
            }
            String[] items = new String[] {getString(R.string.rc_save_picture)};
            OptionsPopupDialog.newInstance(this, items)
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
                                                        PicturePagerActivity.this, permissions)) {
                                            return;
                                        }
                                        ExecutorHelper.getInstance()
                                                .diskIO()
                                                .execute(
                                                        new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                File file;
                                                                if (largeImageUri
                                                                                .getScheme()
                                                                                .startsWith("http")
                                                                        || largeImageUri
                                                                                .getScheme()
                                                                                .startsWith(
                                                                                        "https")) {
                                                                    try {
                                                                        file =
                                                                                Glide.with(
                                                                                                PicturePagerActivity
                                                                                                        .this)
                                                                                        .asFile()
                                                                                        .load(
                                                                                                largeImageUri)
                                                                                        .submit()
                                                                                        .get(
                                                                                                10,
                                                                                                TimeUnit
                                                                                                        .SECONDS);
                                                                    } catch (ExecutionException e) {
                                                                        file = null;
                                                                        RLog.e(
                                                                                TAG,
                                                                                "onOptionsItemClicked",
                                                                                e);
                                                                    } catch (
                                                                            InterruptedException
                                                                                    e) {
                                                                        file = null;
                                                                        RLog.e(
                                                                                TAG,
                                                                                "onOptionsItemClicked",
                                                                                e);
                                                                        // Restore interrupted
                                                                        // state...
                                                                        Thread.currentThread()
                                                                                .interrupt();
                                                                    } catch (TimeoutException e) {
                                                                        file = null;
                                                                        RLog.e(
                                                                                TAG,
                                                                                "onOptionsItemClicked",
                                                                                e);
                                                                    }
                                                                } else if (largeImageUri
                                                                        .getScheme()
                                                                        .startsWith("file")) {
                                                                    file =
                                                                            new File(
                                                                                    largeImageUri
                                                                                            .toString()
                                                                                            .substring(
                                                                                                    7));
                                                                } else {
                                                                    file =
                                                                            new File(
                                                                                    largeImageUri
                                                                                            .toString());
                                                                }
                                                                final String toast;
                                                                if (file != null && file.exists()) {
                                                                    boolean result =
                                                                            KitStorageUtils
                                                                                    .saveMediaToPublicDir(
                                                                                            PicturePagerActivity
                                                                                                    .this,
                                                                                            file,
                                                                                            KitStorageUtils
                                                                                                    .MediaType
                                                                                                    .IMAGE);
                                                                    if (result) {
                                                                        toast =
                                                                                getString(
                                                                                        R.string
                                                                                                .rc_save_picture_at);
                                                                    } else {
                                                                        toast =
                                                                                getString(
                                                                                        R.string
                                                                                                .rc_src_file_not_found);
                                                                    }
                                                                } else {
                                                                    toast =
                                                                            getString(
                                                                                    R.string
                                                                                            .rc_src_file_not_found);
                                                                }
                                                                ExecutorHelper.getInstance()
                                                                        .mainThread()
                                                                        .execute(
                                                                                new Runnable() {
                                                                                    @Override
                                                                                    public void
                                                                                            run() {
                                                                                        ToastUtils
                                                                                                .show(
                                                                                                        PicturePagerActivity
                                                                                                                .this,
                                                                                                        toast,
                                                                                                        Toast
                                                                                                                .LENGTH_SHORT);
                                                                                    }
                                                                                });
                                                            }
                                                        });
                                    }
                                }
                            })
                    .show();
        }
        return true;
    }

    /**
     * 图片长按处理事件
     *
     * @param v 查看图片的PhotoView
     * @param thumbUri 缩略图的Uri地址
     * @param largeImageUri 原图片的Uri地址
     * @return boolean 返回true不会执行默认的处理
     */
    public boolean onPictureLongClick(View v, Uri thumbUri, Uri largeImageUri) {
        return false;
    }

    private static class DestructListener implements RongIMClient.DestructCountDownTimerListener {
        private WeakReference<ImageAdapter.ViewHolder> mHolder;
        private String mMessageId;

        public DestructListener(ImageAdapter.ViewHolder pHolder, String pMessageId) {
            mHolder = new WeakReference<>(pHolder);
            mMessageId = pMessageId;
        }

        @Override
        public void onTick(long millisUntilFinished, String pMessageId) {
            if (mMessageId.equals(pMessageId)) {
                ImageAdapter.ViewHolder viewHolder = mHolder.get();
                if (viewHolder != null) {
                    viewHolder.mCountDownView.setVisibility(View.VISIBLE);
                    viewHolder.mCountDownView.setText(
                            String.valueOf(Math.max(millisUntilFinished, 1)));
                }
            }
        }

        @Override
        public void onStop(String messageId) {
            if (mMessageId.equals(messageId)) {
                ImageAdapter.ViewHolder viewHolder = mHolder.get();
                if (viewHolder != null) {
                    viewHolder.mCountDownView.setVisibility(View.GONE);
                }
            }
        }
    }

    protected class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
        private List<ImageInfo> mImageList = new CopyOnWriteArrayList<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.rc_fr_image, parent, false);
            ViewHolder holder = new ViewHolder(view);

            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            updatePhotoView(position, holder);
            holder.photoView.setOnLongClickListener(PicturePagerActivity.this);
            holder.photoView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Window window = PicturePagerActivity.this.getWindow();
                            if (window != null) {
                                window.setFlags(
                                        WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                                        WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                            }
                            finish();
                        }
                    });
        }

        @Override
        public int getItemCount() {
            return mImageList.size();
        }

        private void updatePhotoView(final int position, final ViewHolder holder) {
            final Uri originalUri = mImageList.get(position).getLargeImageUri();
            // 图片url为file地址、图片url非file地址，并且是私有云，需要在请求私有云token成功后，再请求图片
            if (!FileUtils.uriStartWithFile(originalUri) && RongCoreClientImpl.isPrivateSDK()) {
                RongCoreClient.getInstance()
                        .getMediaUploadAuthorInfo(
                                GlideUtils.getUrlName(originalUri.toString()),
                                originalUri.toString(),
                                new IRongCoreCallback.ResultCallback<MediaUploadAuthorInfo>() {
                                    @Override
                                    public void onSuccess(MediaUploadAuthorInfo auth) {
                                        updatePhotoView(position, holder, auth);
                                    }

                                    @Override
                                    public void onError(IRongCoreEnum.CoreErrorCode e) {
                                        updatePhotoView(position, holder, null);
                                    }
                                });
            } else {
                updatePhotoView(position, holder, null);
            }
        }

        private void updatePhotoView(
                final int position, final ViewHolder holder, MediaUploadAuthorInfo auth) {
            final ImageInfo imageInfo = mImageList.get(position);
            final Uri originalUri = imageInfo.getLargeImageUri();
            final Uri thumbUri = imageInfo.getThumbUri();

            if (originalUri == null || thumbUri == null) {
                RLog.e(TAG, "large uri and thumbnail uri of the image should not be null.");
                return;
            }
            if (mCurrentImageMessage.isDestruct()
                    && mMessage.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                DestructManager.getInstance()
                        .addListener(
                                mMessage.getUId(),
                                new DestructListener(holder, mMessage.getUId()),
                                TAG);
            }
            Glide.with(PicturePagerActivity.this)
                    .asBitmap()
                    .load(GlideUtils.buildAuthUrl(originalUri, auth))
                    .timeout(LOAD_PICTURE_TIMEOUT)
                    .into(
                            new CustomTarget<Bitmap>() {
                                private Runnable mLoadFailedAction = null;

                                @Override
                                public void onResourceReady(
                                        @NonNull Bitmap resource,
                                        @Nullable Transition<? super Bitmap> transition) {
                                    holder.itemView.removeCallbacks(mLoadFailedAction);
                                    int maxLoader = Utils.getMaxLoader(); // openGL最大允许的长或宽
                                    Bitmap desBitmap = null;
                                    if (resource != null
                                            && resource.getWidth() < maxLoader
                                            && resource.getHeight() < maxLoader) {
                                        try {
                                            desBitmap =
                                                    resource.copy(Bitmap.Config.ARGB_8888, true);
                                        } catch (Throwable e) {
                                            RLog.e(TAG, "onResourceReady Bitmap copy error: " + e);
                                        }
                                    }
                                    if (desBitmap != null) {
                                        if (mCurrentImageMessage.isDestruct()
                                                && mMessage.getMessageDirection()
                                                        .equals(Message.MessageDirection.RECEIVE)) {
                                            DestructManager.getInstance().startDestruct(mMessage);
                                        }
                                        holder.progressText.setVisibility(View.GONE);
                                        holder.failImg.setVisibility(View.GONE);
                                        holder.progressBar.setVisibility(View.GONE);
                                        holder.photoView.setVisibility(View.VISIBLE);
                                        holder.photoView.setBitmapAndFileUri(desBitmap, null);
                                        imageInfo.download = true;
                                    } else {
                                        if (FileUtils.uriStartWithFile(originalUri)) {
                                            if (mCurrentImageMessage.isDestruct()
                                                    && mMessage.getMessageDirection()
                                                            .equals(
                                                                    Message.MessageDirection
                                                                            .RECEIVE)) {
                                                DestructManager.getInstance()
                                                        .startDestruct(mMessage);
                                            }
                                            holder.progressText.setVisibility(View.GONE);
                                            holder.failImg.setVisibility(View.GONE);
                                            holder.progressBar.setVisibility(View.GONE);
                                            holder.photoView.setVisibility(View.VISIBLE);
                                            holder.photoView.setBitmapAndFileUri(null, originalUri);
                                            imageInfo.download = true;
                                            return;
                                        }
                                        Glide.with(PicturePagerActivity.this)
                                                .asFile()
                                                .load(originalUri)
                                                .timeout(LOAD_PICTURE_TIMEOUT)
                                                .into(
                                                        new CustomTarget<File>() {
                                                            @Override
                                                            public void onResourceReady(
                                                                    @NonNull File resource,
                                                                    @Nullable
                                                                            Transition<? super File>
                                                                                    transition) {
                                                                if (mCurrentImageMessage
                                                                                .isDestruct()
                                                                        && mMessage.getMessageDirection()
                                                                                .equals(
                                                                                        Message
                                                                                                .MessageDirection
                                                                                                .RECEIVE)) {
                                                                    DestructManager.getInstance()
                                                                            .startDestruct(
                                                                                    mMessage);
                                                                }
                                                                holder.progressText.setVisibility(
                                                                        View.GONE);
                                                                holder.failImg.setVisibility(
                                                                        View.GONE);
                                                                holder.progressBar.setVisibility(
                                                                        View.GONE);
                                                                holder.photoView.setVisibility(
                                                                        View.VISIBLE);
                                                                holder.photoView
                                                                        .setBitmapAndFileUri(
                                                                                null,
                                                                                Uri.fromFile(
                                                                                        resource));
                                                                imageInfo.download = true;
                                                            }

                                                            @Override
                                                            public void onLoadCleared(
                                                                    @Nullable
                                                                            Drawable placeholder) {
                                                                loadFailed(holder);
                                                            }
                                                        });
                                    }
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                    loadFailed(holder);
                                }

                                @Override
                                public void onLoadStarted(@Nullable Drawable placeholder) {
                                    holder.itemView.removeCallbacks(mLoadFailedAction);
                                    String thumbPath = null;
                                    if ("file".equals(thumbUri.getScheme())) {
                                        thumbPath = thumbUri.toString().substring(7);
                                    }
                                    if (thumbPath == null) {
                                        RLog.e(TAG, "thumbPath should not be null.");
                                        return;
                                    }

                                    Bitmap tempBitmap = BitmapFactory.decodeFile(thumbPath);
                                    if (tempBitmap == null) {
                                        RLog.e(TAG, "tempBitmap should not be null.");
                                        return;
                                    }

                                    /*
                                     * 为保持与加载后放大缩小手势可缩放比率一致
                                     * 且防止因与加载成功后方法不同导致偶发加载成功后仍显示缩略图情况
                                     * 所以用 setBitmapAndFileUri 方式加载，不用 setImage 方法
                                     * setBitmapAndFileUri 与 suitMaxScaleWithSize 缩放规则一致
                                     */
                                    holder.photoView.setVisibility(View.VISIBLE);
                                    holder.photoView.setBitmapAndFileUri(tempBitmap, null);
                                    holder.progressBar.setVisibility(View.VISIBLE);
                                    holder.failImg.setVisibility(View.GONE);
                                    holder.progressText.setVisibility(View.GONE);
                                    holder.startLoadTime = SystemClock.elapsedRealtime();
                                }

                                @Override
                                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                    super.onLoadFailed(errorDrawable);
                                    long delayMillis =
                                            (holder.startLoadTime + LOAD_PICTURE_TIMEOUT)
                                                    - SystemClock.elapsedRealtime();
                                    holder.itemView.removeCallbacks(mLoadFailedAction);
                                    if (delayMillis > 0) {
                                        mLoadFailedAction =
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        loadFailed(holder);
                                                    }
                                                };
                                        holder.itemView.postDelayed(mLoadFailedAction, delayMillis);
                                    } else {
                                        loadFailed(holder);
                                    }
                                }
                            });
        }

        private void loadFailed(ViewHolder holder) {
            holder.progressText.setVisibility(View.VISIBLE);
            holder.progressText.setText(R.string.rc_load_image_failed);
            holder.progressBar.setVisibility(View.GONE);
            holder.failImg.setVisibility(View.VISIBLE);
            holder.failImg.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    });
            holder.photoView.setVisibility(View.INVISIBLE);
        }

        public Bitmap zoomImg(Bitmap bm, int newWidth, int newHeight) {
            // 获得图片的宽高
            int width = bm.getWidth();
            int height = bm.getHeight();
            // 计算缩放比例
            float scaleWidth = ((float) newWidth) / width;
            float scaleHeight = ((float) newHeight) / height;
            // 取得想要缩放的matrix参数
            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);
            // 得到新的图片
            Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
            return newbm;
        }

        public void addData(ArrayList<ImageInfo> newImages, boolean direction) {
            if (newImages == null || newImages.size() == 0) return;
            if (direction && !isDuplicate(newImages.get(0).getMessage().getMessageId())) {
                mImageList.addAll(0, newImages);
                notifyItemRangeInserted(0, newImages.size());
            } else if (!direction && !isDuplicate(newImages.get(0).getMessage().getMessageId())) {
                mImageList.addAll(mImageList.size(), newImages);
                notifyItemRangeInserted(mImageList.size(), newImages.size());
            }
        }

        private boolean isDuplicate(int messageId) {
            for (ImageInfo info : mImageList) {
                if (info.getMessage().getMessageId() == messageId) return true;
            }
            return false;
        }

        @Nullable
        public ImageInfo getItem(int index) {
            if (index >= mImageList.size()) {
                return null;
            }
            return mImageList.get(index);
        }

        public int getIndexByMessageId(int messageId) {
            int index = -1;
            for (int i = 0; i < mImageList.size(); i++) {
                if (mImageList.get(i).getMessage().getMessageId() == messageId) {
                    index = i;
                    break;
                }
            }
            return index;
        }

        private void removeRecallItem(int messageId) {
            for (int i = mImageList.size() - 1; i >= 0; i--) {
                if (mImageList.get(i).message.getMessageId() == messageId) {
                    mImageList.remove(i);
                    notifyItemRemoved(i);
                    break;
                }
            }
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ProgressBar progressBar;
            TextView progressText;
            ImageView failImg;
            SubsamplingScaleImageView photoView;
            TextView mCountDownView;
            long startLoadTime;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                progressBar = itemView.findViewById(R.id.rc_progress);
                progressText = itemView.findViewById(R.id.rc_txt);
                failImg = itemView.findViewById(R.id.rc_fail_image);
                photoView = itemView.findViewById(R.id.rc_photoView);
                mCountDownView = itemView.findViewById(R.id.rc_count_down);
            }
        }
    }

    protected class ImageInfo {
        private Message message;
        private Uri thumbUri;
        private Uri largeImageUri;
        private boolean download;

        ImageInfo(Message message, Uri thumbnail, Uri largeImageUri) {
            this.message = message;
            this.thumbUri = thumbnail;
            this.largeImageUri = largeImageUri;
        }

        public Message getMessage() {
            return message;
        }

        public Uri getLargeImageUri() {
            return largeImageUri;
        }

        public Uri getThumbUri() {
            return thumbUri;
        }

        public boolean isDownload() {
            return download;
        }

        public void setDownload(boolean download) {
            this.download = download;
        }
    }
}
