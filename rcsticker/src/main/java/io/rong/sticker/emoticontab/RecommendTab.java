package io.rong.sticker.emoticontab;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.sticker.R;
import io.rong.sticker.StickerExtensionModule;
import io.rong.sticker.businesslogic.StickerPackageDownloadTask;
import io.rong.sticker.businesslogic.StickerPackageSortTask;
import io.rong.sticker.model.StickerPackage;
import io.rong.sticker.util.DownloadUtil;
import io.rong.sticker.widget.DownloadProgressView;
import io.rong.sticker.widget.IndicatorView;

import static io.rong.sticker.StickerExtensionModule.sRongExtensionWeakReference;

/**
 * Created by luoyanlong on 2018/08/09.
 * 展示多个未下载的表情包
 */
public class RecommendTab implements IEmoticonTab {
    private final String EXTENSION_TAG = StickerExtensionModule.class.getSimpleName();
    private List<StickerPackage> packages;
    private IndicatorView indicatorView;
    private ViewPager2 viewPager;
    private RecommendPackageAdapter adapter;
    private Handler uiHandler;

    public RecommendTab(List<StickerPackage> packages) {
        this.packages = packages;
    }

    @Override
    public Drawable obtainTabDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_icon_recommend);
    }

    @Override
    public View obtainTabPager(Context context, ViewGroup viewGroup) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_sticker_download, viewGroup, false);
        uiHandler = new Handler();
        viewPager = view.findViewById(R.id.download_view_pager);
        adapter = new RecommendPackageAdapter(packages);
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                indicatorView.setSelect(position);
            }
        });
        indicatorView = view.findViewById(R.id.indicator_view);
        indicatorView.setCount(adapter.getItemCount());
        return view;
    }

    @Override
    public void onTableSelected(int position) {

    }

    @Override
    public LiveData<String> getEditInfo() {
        return null;
    }

    public void setPackages(List<StickerPackage> packages) {
        this.packages = packages;
        adapter = new RecommendPackageAdapter(packages);
        viewPager.setAdapter(adapter);
        indicatorView.setCount(adapter.getItemCount());
    }

    public void removePackage(StickerPackage stickerPackage) {
        boolean isChanged = false;
        Iterator<StickerPackage> iterator = packages.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getPackageId().equals(stickerPackage.getPackageId())) {
                iterator.remove();
                isChanged = true;
                break;
            }
        }
        if (adapter != null && isChanged) {
            adapter.setPackages(packages);
            adapter.notifyDataSetChanged();
            indicatorView.setCount(adapter.getItemCount());
        }
    }

    public boolean isEmpty() {
        return adapter.getItemCount() == 0;
    }

    private class RecommendPackageAdapter extends RecyclerView.Adapter<RecommendViewHolder> {

        private List<StickerPackage> packages;

        RecommendPackageAdapter(List<StickerPackage> packages) {
            this.packages = packages;
        }

        void setPackages(List<StickerPackage> packages) {
            this.packages = packages;
        }

        @NonNull
        @Override
        public RecommendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ViewGroup stickerView = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_sticker_download_page, parent, false);
            return new RecommendViewHolder(parent.getContext(), stickerView);
        }

        @Override
        public void onBindViewHolder(@NonNull final RecommendViewHolder holder, int position) {
            final StickerPackage stickerPackage = packages.get(position);
            Glide.with(holder.context).load(stickerPackage.getCover()).into(holder.icon);
            holder.description.setText(stickerPackage.getName());
            holder.downloadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StickerPackageDownloadTask task =
                            new StickerPackageDownloadTask(holder.context, stickerPackage.getPackageId());
                    task.downloadStickerPackage(new DownloadUtil.DownloadListener() {
                        @Override
                        public void onProgress(final int progress) {
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    holder.downloadBtn.setProgress(progress);
                                }
                            });
                        }

                        @Override
                        public void onComplete(String path) {

                        }

                        @Override
                        public void onError(Exception e) {
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    holder.downloadBtn.setStatus(DownloadProgressView.NOT_DOWNLOAD);
                                    String content = holder.context.getResources().getString(R.string.sticker_download_fail, stickerPackage.getName());
                                    Toast.makeText(holder.context, content, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }, new StickerPackageDownloadTask.ZipListener() {
                        @Override
                        public void onUnzip(final StickerPackage unzipPackage) {
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    addPackageToEmoticonBoard(unzipPackage);
                                }
                            });
                        }
                    });
                }
            });
        }

        @Override
        public int getItemCount() {
            return packages == null ? 0 : packages.size();
        }
    }

    private void addPackageToEmoticonBoard(StickerPackage stickerPackage) {
        if (sRongExtensionWeakReference != null) {
            RongExtension rongExtension = sRongExtensionWeakReference.get();
            if (rongExtension != null) {
                StickersTab stickersTab = new StickersTab(stickerPackage);
                List<IEmoticonTab> tabList = rongExtension.getEmoticonBoard().getTabList(EXTENSION_TAG);
                int index = StickerPackageSortTask.getInsertIndex(tabList, stickerPackage);
                rongExtension.getEmoticonBoard().addTab(index + 1, stickersTab, EXTENSION_TAG);
                rongExtension.getEmoticonBoard().setCurrentTab(stickersTab, EXTENSION_TAG);
                if (stickerPackage.isPreload() == 0) {
                    this.removePackage(stickerPackage);
                    if (isEmpty()) {
                        rongExtension.getEmoticonBoard().removeTab(this, EXTENSION_TAG);
                    }
                }
            }
        }
    }

    private class RecommendViewHolder extends RecyclerView.ViewHolder {
        private Context context;
        private ImageView icon;
        private TextView description;
        private DownloadProgressView downloadBtn;

        public RecommendViewHolder(Context context, @NonNull ViewGroup itemView) {
            super(itemView);
            this.context = context;
            icon = itemView.findViewById(R.id.iv_cover);
            description = itemView.findViewById(R.id.package_name);
            downloadBtn = itemView.findViewById(R.id.btn);
        }
    }
}
