package io.rong.imkit.picture.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.picture.config.PictureSelectionConfig;
import io.rong.imkit.picture.entity.LocalMedia;
import io.rong.imkit.picture.entity.LocalMediaFolder;

/**
 * @author：luck
 * @date：2016-12-11 17:02
 * @describe：文件夹目录
 */
public class PictureAlbumDirectoryAdapter extends RecyclerView.Adapter<PictureAlbumDirectoryAdapter.ViewHolder> {
    private List<LocalMediaFolder> folders = new ArrayList<>();
    private PictureSelectionConfig config;

    public PictureAlbumDirectoryAdapter(PictureSelectionConfig config) {
        super();
        this.config = config;
    }

    public void bindFolderData(List<LocalMediaFolder> folders) {
        this.folders = folders;
        notifyDataSetChanged();
    }


    public List<LocalMediaFolder> getFolderData() {
        if (folders == null) {
            folders = new ArrayList<>();
        }
        return folders;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rc_picture_album_folder_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final LocalMediaFolder folder = folders.get(position);
        String name = folder.getName();
        int imageNum = folder.getImageNum();
        String imagePath = folder.getFirstImagePath();
        boolean isChecked = folder.isChecked();
        int checkedNum = folder.getCheckedNum();
        holder.tvSign.setVisibility(checkedNum > 0 ? View.VISIBLE : View.INVISIBLE);
        holder.itemView.setSelected(isChecked);
        if (config != null && config.imageEngine != null) {
            config.imageEngine
                    .loadFolderImage(holder.itemView.getContext(),
                            imagePath, holder.ivFirstImage);
        }
        Context context = holder.itemView.getContext();
        String firstTitle = folder.getOfAllType() != -1 ? context.getString(R.string.picture_camera_roll) : name;
        holder.tvFolderName.setText(context.getString(R.string.picture_camera_roll_num, firstTitle, imageNum));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    for (LocalMediaFolder mediaFolder : folders) {
                        mediaFolder.setChecked(false);
                    }
                    folder.setChecked(true);
                    notifyDataSetChanged();
                    onItemClickListener.onItemClick(folder.isCameraFolder(), folder.getName(), folder.getImages());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFirstImage;
        TextView tvFolderName, tvSign;

        public ViewHolder(View itemView) {
            super(itemView);
            ivFirstImage = itemView.findViewById(R.id.first_image);
            tvFolderName = itemView.findViewById(R.id.tv_folder_name);
            tvSign = itemView.findViewById(R.id.tv_sign);
        }
    }

    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(boolean isCameraFolder, String folderName, List<LocalMedia> images);
    }
}
