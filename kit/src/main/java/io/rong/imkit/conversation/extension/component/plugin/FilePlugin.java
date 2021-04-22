package io.rong.imkit.conversation.extension.component.plugin;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import io.rong.common.FileUtils;
import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imkit.utils.KitStorageUtils;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.message.FileMessage;

public class FilePlugin implements IPluginModule {

    private static final String TAG = "FilePlugin";
    private static final int REQUEST_FILE = 100;
    // 发送消息间隔
    private static final int TIME_DELAY = 400;
    private Conversation.ConversationType conversationType;
    private String targetId;
    private Context mContext;

    @Override
    public Drawable obtainDrawable(Context context) {
        return ContextCompat.getDrawable(context, R.drawable.rc_ic_files_selector);
    }

    @Override
    public String obtainTitle(Context context) {
        return context.getString(R.string.rc_ext_plugin_file);
    }

    @Override
    public void onClick(Fragment currentFragment, RongExtension extension, int index) {
        conversationType = extension.getConversationType();
        targetId = extension.getTargetId();
        FragmentActivity activity = currentFragment.getActivity();
        if (activity != null) {
            mContext = activity.getApplicationContext();
        }

        //Android11以上设备(无论target多少)不可访问Android/data/...和Android/obb/...目录及其所有子目录,影响不大,忽略.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        extension.startActivityForPluginResult(intent, REQUEST_FILE, this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_FILE) {
            if (data != null) {
                final Uri uri = data.getData();
                ExecutorHelper.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String displayName = DocumentFile.fromSingleUri(mContext, uri).getName();
                            String fileSavePath = KitStorageUtils.getFileSavePath(mContext);
                            if (FileUtils.copyFileToInternal(mContext, uri, fileSavePath, displayName)) {
                                FileMessage fileMessage = FileMessage.obtain(mContext, Uri.parse("file://" + fileSavePath + "/" + displayName));
                                if (fileMessage != null) {
                                    final Message message = Message.obtain(targetId, conversationType, fileMessage);
                                    IMCenter.getInstance().sendMediaMessage(message, null, null, (IRongCallback.ISendMediaMessageCallback) null);
                                }
                            } else {
                                RLog.e(TAG, "copy file error,uri is " + uri.toString());
                            }
                        } catch (Exception e) {
                            RLog.e(TAG, "select file exception" + e);
                        }
                    }
                });

            }
        }
    }


}
