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
import android.os.Bundle;
import android.os.PersistableBundle;
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

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.event.actionevent.BaseMessageEvent;
import io.rong.imkit.event.actionevent.DeleteEvent;
import io.rong.imkit.event.actionevent.RecallEvent;
import io.rong.imkit.feature.destruct.DestructManager;
import io.rong.imkit.picture.widget.longimage.ImageSource;
import io.rong.imkit.picture.widget.longimage.SubsamplingScaleImageView;
import io.rong.imkit.picture.widget.longimage.Utils;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imkit.utils.KitStorageUtils;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.widget.dialog.OptionsPopupDialog;
import io.rong.imlib.RongCommonDefine;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.message.ImageMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;

public class PicturePagerActivity extends RongBaseNoActionbarActivity implements View.OnLongClickListener {
    private static final String TAG = "PicturePagerActivity";
    private static final int IMAGE_MESSAGE_COUNT = 10; //每次获取的图片消息数量。
    protected ViewPager2 mViewPager;
    protected ImageMessage mCurrentImageMessage;
    protected Message mMessage;
    protected Conversation.ConversationType mConversationType;
    protected int mCurrentMessageId;
    protected String mTargetId = null;
    protected ImageAdapter mImageAdapter;
    protected boolean isFirstTime = false;

    protected ViewPager2.OnPageChangeCallback mPageChangeListener = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            if (position == (mImageAdapter.getItemCount() - 1)) {
                if (mImageAdapter.getItemCount() > 0) {
                    getConversationImageUris(mImageAdapter.getItem(position).getMessage().getMessageId(), RongCommonDefine.GetMessageDirection.BEHIND);
                }
            } else if (position == 0) {
                if (mImageAdapter.getItemCount() > 0) {
                    getConversationImageUris(mImageAdapter.getItem(position).getMessage().getMessageId(), RongCommonDefine.GetMessageDirection.FRONT);
                }
            }
        }
    };
    RongIMClient.OnRecallMessageListener mOnRecallMessageListener = new RongIMClient.OnRecallMessageListener() {
        @Override
        public boolean onMessageRecalled(Message message, RecallNotificationMessage recallNotificationMessage) {
            if (mCurrentMessageId == message.getMessageId()) {
                new AlertDialog.Builder(PicturePagerActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                        .setMessage(getString(R.string.rc_recall_success))
                        .setPositiveButton(getString(R.string.rc_dialog_ok), new DialogInterface.OnClickListener() {

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
    BaseMessageEvent mBaseMessageEvent = new BaseMessageEvent() {
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
            if (mCurrentMessageId == event.getMessageId()) {
                new AlertDialog.Builder(PicturePagerActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                        .setMessage(getString(R.string.rc_recall_success))
                        .setPositiveButton(getString(R.string.rc_dialog_ok), new DialogInterface.OnClickListener() {

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
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
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
        mTargetId = currentMessage.getTargetId();

        mViewPager = findViewById(R.id.viewpager);
        mViewPager.registerOnPageChangeCallback(mPageChangeListener);
        mImageAdapter = new ImageAdapter();
        isFirstTime = true;
        if (!(mMessage.getContent().isDestruct() || mMessage.getContent() instanceof ReferenceMessage)) {
            getConversationImageUris(mCurrentMessageId, RongCommonDefine.GetMessageDirection.FRONT);  //获取当前点开图片之前的图片消息。
            getConversationImageUris(mCurrentMessageId, RongCommonDefine.GetMessageDirection.BEHIND);
        } else {
            //阅后即焚和引用消息只显示1张照片
            ArrayList<ImageInfo> lists = new ArrayList<>();
            lists.add(new ImageInfo(mMessage, mCurrentImageMessage.getThumUri(),
                    mCurrentImageMessage.getLocalUri() == null ? mCurrentImageMessage.getRemoteUri() : mCurrentImageMessage.getLocalUri()));
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

    private void getConversationImageUris(int messageId, final RongCommonDefine.GetMessageDirection direction) {
        if (mConversationType != null && !TextUtils.isEmpty(mTargetId)) {
            RongIMClient.getInstance().getHistoryMessages(mConversationType, mTargetId, "RC:ImgMsg", messageId, IMAGE_MESSAGE_COUNT, direction, new RongIMClient.ResultCallback<List<Message>>() {
                @Override
                public void onSuccess(List<Message> messages) {
                    int i;
                    ArrayList<ImageInfo> lists = new ArrayList<>();
                    if (messages != null) {
                        if (direction.equals(RongCommonDefine.GetMessageDirection.FRONT)) {
                            Collections.reverse(messages);
                        }
                        for (i = 0; i < messages.size(); i++) {
                            Message message = messages.get(i);
                            if (message.getContent() instanceof ImageMessage && !message.getContent().isDestruct()) {
                                ImageMessage imageMessage = (ImageMessage) message.getContent();
                                Uri largeImageUri = imageMessage.getLocalUri() == null ? imageMessage.getRemoteUri() : imageMessage.getLocalUri();

                                if (imageMessage.getThumUri() != null && largeImageUri != null) {
                                    lists.add(new ImageInfo(message, imageMessage.getThumUri(), largeImageUri));
                                }
                            }
                        }
                    }
                    if (direction.equals(RongCommonDefine.GetMessageDirection.FRONT)) {
                        if (isFirstTime) {
                            lists.add(new ImageInfo(mMessage, mCurrentImageMessage.getThumUri(),
                                    mCurrentImageMessage.getLocalUri() == null ? mCurrentImageMessage.getRemoteUri() : mCurrentImageMessage.getLocalUri()));
                        }
                        mImageAdapter.addData(lists, true);
                        if (isFirstTime) {
                            int index = mImageAdapter.getIndexByMessageId(mMessage.getMessageId());
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

                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {

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
            String[] items = new String[]{getString(R.string.rc_save_picture)};
            OptionsPopupDialog.newInstance(this, items).setOptionsPopupDialogListener(new OptionsPopupDialog.OnOptionsItemClickedListener() {
                @Override
                public void onOptionsItemClicked(int which) {
                    if (which == 0) {
                        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        if (!PermissionCheckUtil.requestPermissions(PicturePagerActivity.this, permissions)) {
                            return;
                        }
                        ExecutorHelper.getInstance().diskIO().execute(new Runnable() {
                            @Override
                            public void run() {
                                File file;
                                if (largeImageUri.getScheme().startsWith("http") || largeImageUri.getScheme().startsWith("https")) {
                                    try {
                                        file = Glide.with(PicturePagerActivity.this).asFile().load(largeImageUri).submit().get(10, TimeUnit.SECONDS);
                                    } catch (ExecutionException e) {
                                        file = null;
                                        RLog.e(TAG, "onOptionsItemClicked", e);
                                    } catch (InterruptedException e) {
                                        file = null;
                                        RLog.e(TAG, "onOptionsItemClicked", e);
                                        // Restore interrupted state...
                                        Thread.currentThread().interrupt();
                                    } catch (TimeoutException e) {
                                        file = null;
                                        RLog.e(TAG, "onOptionsItemClicked", e);
                                    }
                                } else if (largeImageUri.getScheme().startsWith("file")) {
                                    file = new File(largeImageUri.toString().substring(7));
                                } else {
                                    file = new File(largeImageUri.toString());
                                }
                                final String toast;
                                if (file != null && file.exists()) {
                                    boolean result = KitStorageUtils.saveMediaToPublicDir(PicturePagerActivity.this, file, KitStorageUtils.MediaType.IMAGE);
                                    if (result) {
                                        toast = getString(R.string.rc_save_picture_at);
                                    } else {
                                        toast = getString(R.string.rc_src_file_not_found);
                                    }
                                } else {
                                    toast = getString(R.string.rc_src_file_not_found);
                                }
                                ExecutorHelper.getInstance().mainThread().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(PicturePagerActivity.this, toast, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });

                    }
                }
            }).show();
        }
        return true;
    }

    /**
     * 图片长按处理事件
     *
     * @param v             查看图片的PhotoView
     * @param thumbUri      缩略图的Uri地址
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
                    viewHolder.mCountDownView.setText(String.valueOf(Math.max(millisUntilFinished, 1)));
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
        private ArrayList<ImageInfo> mImageList = new ArrayList<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_fr_image, parent, false);
            ViewHolder holder = new ViewHolder(view);

            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            updatePhotoView(position, holder);
            holder.photoView.setOnLongClickListener(PicturePagerActivity.this);
            holder.photoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Window window = PicturePagerActivity.this.getWindow();
                    if (window != null) {
                        window.setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
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
            final ImageInfo imageInfo = mImageList.get(position);
            final Uri originalUri = imageInfo.getLargeImageUri();
            final Uri thumbUri = imageInfo.getThumbUri();

            if (originalUri == null || thumbUri == null) {
                RLog.e(TAG, "large uri and thumbnail uri of the image should not be null.");
                return;
            }
            if (mCurrentImageMessage.isDestruct() && mMessage.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                DestructManager.getInstance().addListener(mMessage.getUId(), new DestructListener(holder, mMessage.getUId()), TAG);
            }
            Glide.with(PicturePagerActivity.this).asBitmap().load(originalUri).timeout(30000).into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    resource = resource.copy(Bitmap.Config.ARGB_8888, true);
                    int maxLoader = Utils.getMaxLoader();//openGL最大允许的长或宽
                    if (resource != null && resource.getWidth() < maxLoader && resource.getHeight() < maxLoader) {
                        if (mCurrentImageMessage.isDestruct() && mMessage.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                            DestructManager.getInstance().startDestruct(mMessage);
                            //todo
                            //EventBus.getDefault().post(new Event.changeDestructionReadTimeEvent(mMessage));
                        }
                        holder.progressText.setVisibility(View.GONE);
                        holder.failImg.setVisibility(View.GONE);
                        holder.progressBar.setVisibility(View.GONE);
                        holder.photoView.setVisibility(View.VISIBLE);
                        holder.photoView.setBitmapAndFileUri(resource, null);
                        imageInfo.download = true;
                    } else {
                        Glide.with(PicturePagerActivity.this).asFile().load(originalUri).timeout(30000).into(new CustomTarget<File>() {
                            @Override
                            public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {

                                if (mCurrentImageMessage.isDestruct() && mMessage.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                                    DestructManager.getInstance().startDestruct(mMessage);
                                    //todo
                                    //EventBus.getDefault().post(new Event.changeDestructionReadTimeEvent(mMessage));
                                }
                                holder.progressText.setVisibility(View.GONE);
                                holder.failImg.setVisibility(View.GONE);
                                holder.progressBar.setVisibility(View.GONE);
                                holder.photoView.setVisibility(View.VISIBLE);
                                holder.photoView.setBitmapAndFileUri(null, Uri.fromFile(resource));
                                imageInfo.download = true;
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                holder.progressText.setVisibility(View.VISIBLE);
                                holder.progressText.setText(R.string.rc_load_image_failed);
                                holder.progressBar.setVisibility(View.GONE);
                                holder.failImg.setVisibility(View.VISIBLE);
                                holder.failImg.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        finish();
                                    }
                                });
                                holder.photoView.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                    holder.progressText.setVisibility(View.VISIBLE);
                    holder.progressText.setText(R.string.rc_load_image_failed);
                    holder.progressBar.setVisibility(View.GONE);
                    holder.failImg.setVisibility(View.VISIBLE);
                    holder.failImg.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    });
                    holder.photoView.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onLoadStarted(@Nullable Drawable placeholder) {
                    String thumbPath = null;
                    Bitmap thumbBitmap = null;
                    if ("file".equals(thumbUri.getScheme())) {
                        thumbPath = thumbUri.toString().substring(7);
                    }
                    if (thumbPath != null) {
                        thumbBitmap = BitmapFactory.decodeFile(thumbPath).copy(Bitmap.Config.ARGB_8888, true);
                    }
                    /*
                     * 为保持与加载后放大缩小手势可缩放比率一致
                     * 且防止因与加载成功后方法不同导致偶发加载成功后仍显示缩略图情况
                     * 所以用 setBitmapAndFileUri 方式加载，不用 setImage 方法
                     * setBitmapAndFileUri 与 suitMaxScaleWithSize 缩放规则一致
                     */
                    holder.photoView.setVisibility(View.VISIBLE);
                    if (thumbBitmap != null) {
                        holder.photoView.setImage(ImageSource.bitmap(thumbBitmap));
                    }
                    holder.progressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    super.onLoadFailed(errorDrawable);
                    holder.progressText.setVisibility(View.VISIBLE);
                    holder.progressText.setText(R.string.rc_load_image_failed);
                    holder.progressBar.setVisibility(View.GONE);
                    holder.failImg.setVisibility(View.VISIBLE);
                    holder.failImg.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    });
                    holder.photoView.setVisibility(View.INVISIBLE);
                }
            });
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
            if (newImages == null || newImages.size() == 0)
                return;
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
                if (info.getMessage().getMessageId() == messageId)
                    return true;
            }
            return false;
        }

        public ImageInfo getItem(int index) {
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
