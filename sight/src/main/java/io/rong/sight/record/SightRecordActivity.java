package io.rong.sight.record;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.WindowManager;
import io.rong.common.RLog;
import io.rong.imkit.activity.RongBaseNoActionbarActivity;
import io.rong.imkit.utils.KitStorageUtils;
import io.rong.sight.R;
import java.io.File;

public class SightRecordActivity extends RongBaseNoActionbarActivity {
    public static final String TAG = "Sight-SightRecordActivity";
    private CameraView mCameraView;
    private static final int DEFAULT_MAX_RECORD_DURATION = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow()
                .setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.rc_activity_sight_record);

        mCameraView = findViewById(R.id.cameraView);
        mCameraView.setAutoFocus(false);
        mCameraView.setSupportCapture(getIntent().getBooleanExtra("supportCapture", false));
        mCameraView.setSaveVideoPath(getIntent().getStringExtra("recordSightDir"));
        int maxRecordDuration =
                getIntent().getIntExtra("maxRecordDuration", DEFAULT_MAX_RECORD_DURATION);
        if (maxRecordDuration <= 0) {
            maxRecordDuration = DEFAULT_MAX_RECORD_DURATION;
        }
        mCameraView.setMaxRecordDuration(maxRecordDuration);
        mCameraView.setCameraViewListener(
                new CameraView.CameraViewListener() {
                    @Override
                    public void quit() {
                        // default implementation ignored
                    }

                    @Override
                    public void captureSuccess(Bitmap bitmap) {
                        // default implementation ignored
                    }

                    @Override
                    public void recordSuccess(String url, int recordTime) {
                        if (TextUtils.isEmpty(url)) {
                            setResult(RESULT_CANCELED);
                            SightRecordActivity.this.finish();
                            return;
                        }
                        File file = new File(url);
                        if (!file.exists()) {
                            setResult(RESULT_CANCELED);
                            SightRecordActivity.this.finish();
                            return;
                        }
                        boolean result =
                                KitStorageUtils.saveMediaToPublicDir(
                                        SightRecordActivity.this,
                                        file,
                                        KitStorageUtils.MediaType.VIDEO);
                        RLog.i(TAG, "RecordSuccess save result" + result);
                        Intent intent = new Intent();
                        intent.putExtra("recordSightUrl", url);
                        intent.putExtra("recordSightTime", recordTime);
                        setResult(RESULT_OK, intent);
                        SightRecordActivity.this.finish();
                    }

                    @Override
                    public void finish() {
                        SightRecordActivity.this.finish();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraView.onPause();
    }

    @Override
    public void finish() {
        super.finish();
    }
}
