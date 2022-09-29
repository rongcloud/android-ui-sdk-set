package io.rong.sticker.mysticker;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import io.rong.imkit.activity.RongBaseActivity;
import io.rong.sticker.R;
import io.rong.sticker.businesslogic.StickerPackageDbTask;
import io.rong.sticker.model.StickerPackage;
import java.util.List;

/** Created by luoyanlong on 2018/08/17. 删除表情包Activity */
public class MyStickerActivity extends RongBaseActivity {

    private View contentView;
    private View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rc_my_sticker);
        mTitleBar.setTitle(R.string.my_sticker);
        contentView = findViewById(R.id.content_view);
        emptyView = findViewById(R.id.empty_view);
        ListView listView = findViewById(R.id.rc_list);
        List<StickerPackage> list = StickerPackageDbTask.getInstance().getDownloadPackages();
        if (list.isEmpty()) {
            showEmptyView();
        } else {
            showContentView();
            MyStickerListAdapter adapter = new MyStickerListAdapter(this, list);
            adapter.setOnNoStickerListener(
                    new MyStickerListAdapter.OnNoStickerListener() {
                        @Override
                        public void onNoSticker() {
                            showEmptyView();
                        }
                    });
            listView.setAdapter(adapter);
        }
    }

    private void showContentView() {
        contentView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    private void showEmptyView() {
        contentView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }
}
