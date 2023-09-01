package io.rong.contactcard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import io.rong.common.rlog.RLog;
import io.rong.contactcard.activities.ContactDetailActivity;
import io.rong.contactcard.activities.ContactListActivity;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.UserInfo;

/** Created by Beyond on 2016/11/14. */
public class ContactCardPlugin implements IPluginModule {

    private static final String TAG = "ContactCardPlugin";
    private static final int REQUEST_CONTACT = 55;
    private Context context;
    private Conversation.ConversationType conversationType;
    private String targetId;
    public static final String IS_FROM_CARD = "isFromCard";

    public ContactCardPlugin() {
        // do nothing
    }

    @Override
    public Drawable obtainDrawable(Context context) {
        this.context = context;
        return ContextCompat.getDrawable(context, R.drawable.rc_contact_plugin_icon);
    }

    @Override
    public String obtainTitle(Context context) {
        return context.getString(R.string.rc_plugins_contact);
    }

    @Override
    public void onClick(Fragment currentFragment, RongExtension extension, int index) {
        if (extension == null) {
            RLog.e(TAG, "extension or Fragment null");
            return;
        }
        if (currentFragment.getActivity() != null) {
            context = currentFragment.getActivity().getApplicationContext();
        }

        conversationType = extension.getConversationType();
        targetId = extension.getTargetId();

        IContactCardSelectListProvider iContactCardSelectListProvider =
                ContactCardContext.getInstance().getContactCardSelectListProvider();
        IContactCardInfoProvider iContactInfoProvider =
                ContactCardContext.getInstance().getContactCardInfoProvider();
        if (iContactCardSelectListProvider != null) {
            iContactCardSelectListProvider.onContactPluginClick(
                    REQUEST_CONTACT, currentFragment, extension, this);
            extension.collapseExtension();
            return;
        }
        if (context == null) {
            RLog.e(TAG, "context null");
            return;
        }
        if (iContactInfoProvider != null) {
            Intent intent = new Intent(context, ContactListActivity.class);
            extension.startActivityForPluginResult(intent, REQUEST_CONTACT, this);
            intent.putExtra(IS_FROM_CARD, true);
            extension.collapseExtension();
        } else {
            ToastUtils.show(
                    context,
                    "The related interface of \"business card module\" has not been implemented",
                    Toast.LENGTH_LONG);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CONTACT && resultCode == Activity.RESULT_OK) {
            if (conversationType == null || context == null) {
                RLog.e(TAG, "conversationType or context null");
                return;
            }
            Intent intent = new Intent(context, ContactDetailActivity.class);
            intent.putExtra("contact", (UserInfo) data.getParcelableExtra("contact"));
            intent.putExtra("conversationType", conversationType);
            intent.putExtra("targetId", targetId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
