package io.rong.imkit.picture.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.picture.PictureVideoPlayActivity;
import io.rong.imkit.picture.config.PictureMimeType;
import io.rong.imkit.picture.config.PictureSelectionConfig;
import io.rong.imkit.picture.entity.LocalMedia;
import io.rong.imkit.picture.photoview.OnViewTapListener;
import io.rong.imkit.picture.photoview.PhotoView;
import io.rong.imkit.picture.tools.MediaUtils;
import io.rong.imkit.picture.tools.SdkVersionUtils;
import io.rong.imkit.picture.widget.longimage.ImageSource;
import io.rong.imkit.picture.widget.longimage.ImageViewState;
import io.rong.imkit.picture.widget.longimage.SubsamplingScaleImageView;

/**
 * @author：luck
 * @data：2018/1/27 下午7:50
 * @描述:图片预览
 */

public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.PictureViewHolder> {
    private List<LocalMedia> images;
    private Context mContext;
    private OnCallBackActivity onBackPressed;
    private PictureSelectionConfig config;

    public interface OnCallBackActivity {
        /**
         * 关闭预览Activity
         */
        void onActivityBackPressed();
    }

    public ViewPagerAdapter(PictureSelectionConfig config, List<LocalMedia> images, Context context,
                            OnCallBackActivity onBackPressed) {
        super();
        this.config = config;
        this.images = images;
        this.mContext = context;
        this.onBackPressed = onBackPressed;
    }

    @NonNull
    @Override
    public PictureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View contentView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rc_picture_image_preview, parent, false);
        return new PictureViewHolder(contentView);
    }

    @Override
    public void onBindViewHolder(@NonNull PictureViewHolder holder, int position) {
        LocalMedia media = images.get(position);
        if (media != null) {
            final String mimeType = media.getMimeType();
            boolean eqVideo = PictureMimeType.eqVideo(mimeType);
            holder.iv_play.setVisibility(eqVideo ? View.VISIBLE : View.GONE);
            final String path;
            if (media.isCut() && !media.isCompressed()) {
                // 裁剪过
                path = media.getCutPath();
            } else if (media.isCompressed() || (media.isCut() && media.isCompressed())) {
                // 压缩过,或者裁剪同时压缩过,以最终压缩过图片为准
                path = media.getCompressPath();
            } else {
                path = media.getPath();
            }
            boolean isGif = PictureMimeType.isGif(mimeType);
            final boolean eqLongImg = MediaUtils.isLongImg(media);
            holder.imageView.setVisibility(eqLongImg && !isGif ? View.GONE : View.VISIBLE);
            holder.longImg.setVisibility(eqLongImg && !isGif ? View.VISIBLE : View.GONE);
            // 压缩过的gif就不是gif了
            if (isGif && !media.isCompressed()) {
                if (config != null && config.imageEngine != null) {
                    config.imageEngine.loadAsGifImage
                            (holder.imageView.getContext(), path, holder.imageView);
                }
            } else {
                if (config != null && config.imageEngine != null) {
                    if (eqLongImg) {
                        displayLongPic(SdkVersionUtils.checkedAndroid_Q()
                                ? Uri.parse(path) : Uri.fromFile(new File(path)), holder.longImg);
                    } else {
                        config.imageEngine.loadImage
                                (holder.imageView.getContext(), path, holder.imageView);
                    }
                }
            }
            holder.imageView.setOnViewTapListener(new OnViewTapListener() {
                @Override
                public void onViewTap(View view, float x, float y) {
                    if (onBackPressed != null) {
                        onBackPressed.onActivityBackPressed();
                    }
                }
            });
            holder.longImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onBackPressed != null) {
                        onBackPressed.onActivityBackPressed();
                    }
                }
            });
            holder.iv_play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    Bundle bundle = new Bundle();
                    bundle.putString("video_path", path);
                    intent.putExtras(bundle);
                    intent.setClass(mContext, PictureVideoPlayActivity.class);
                    mContext.startActivity(intent);
                }
            });

        }
    }

    @Override
    public int getItemCount() {
        return images != null ? images.size() : 0;
    }


    /**
     * 加载长图
     *
     * @param uri
     * @param longImg
     */
    private void displayLongPic(Uri uri, SubsamplingScaleImageView longImg) {
        longImg.setImage(ImageSource.uri(uri));
    }

    public static class PictureViewHolder extends RecyclerView.ViewHolder {
        PhotoView imageView;
        SubsamplingScaleImageView longImg;
        ImageView iv_play;

        public PictureViewHolder(@NonNull View itemView) {
            super(itemView);
            // 常规图控件
            imageView = itemView.findViewById(R.id.preview_image);
            // 长图控件
            longImg = itemView.findViewById(R.id.longImg);
            iv_play = itemView.findViewById(R.id.iv_play);
        }
    }
}
