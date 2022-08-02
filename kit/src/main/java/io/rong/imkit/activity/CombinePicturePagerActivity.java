package io.rong.imkit.activity;

import android.os.Bundle;

import java.util.ArrayList;

import io.rong.imkit.R;
import io.rong.imlib.model.Message;
import io.rong.message.ImageMessage;
import io.rong.message.ReferenceMessage;

public class CombinePicturePagerActivity extends PicturePagerActivity {
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
        //只显示1张照片
        ArrayList<ImageInfo> lists = new ArrayList<>();
        lists.add(new ImageInfo(mMessage, mCurrentImageMessage.getThumUri(),
                mCurrentImageMessage.getLocalUri() == null ? mCurrentImageMessage.getRemoteUri() : mCurrentImageMessage.getLocalUri()));
        mImageAdapter.addData(lists, true);
        mViewPager.setAdapter(mImageAdapter);

    }
}
