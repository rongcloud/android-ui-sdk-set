package io.rong.sight.record;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;

import java.io.File;

import io.rong.imkit.activity.RongBaseNoActionbarActivity;
import io.rong.sight.R;

public class SightRecordActivity extends RongBaseNoActionbarActivity {
    public static final String TAG = "Sight-SightRecordActivity";
    private CameraView mCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.rc_activity_sight_record);

        mCameraView = findViewById(R.id.cameraView);
        mCameraView.setAutoFocus(false);
        mCameraView.setSupportCapture(getIntent().getBooleanExtra("supportCapture", false));
        mCameraView.setSaveVideoPath(getIntent().getStringExtra("recordSightDir"));
        mCameraView.setMaxRecordDuration(getIntent().getIntExtra("maxRecordDuration", 10));
        mCameraView.setCameraViewListener(new CameraView.CameraViewListener() {
            @Override
            public void quit() {
                SightRecordActivity.this.finish();
            }

            @Override
            public void captureSuccess(Bitmap bitmap) {
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
                Intent intent = new Intent();
                intent.putExtra("recordSightUrl", url);
                intent.putExtra("recordSightTime", recordTime);
                setResult(RESULT_OK, intent);
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
}
