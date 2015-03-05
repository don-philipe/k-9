package de.tud.smime.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
/**
 * Created by don on 12.02.15.
 * from https://stackoverflow.com/questions/5425567/does-the-k9-mail-client-provide-a-way-to-get-the-contents-of-mail-through-a-broa
 */
public class EmailReceiver extends BroadcastReceiver {

    private static final Uri uri = Uri.parse("content://com.fsck.k9.messageprovider/inbox_messages/");
    private static String[] messages_projection = new String[] {"_id", "date", "sender", "subject", "preview", "account", "uri", "delUri"};

    @Override
    public void onReceive(Context context, Intent intent) {
        Cursor cur = context.getContentResolver().query(uri, messages_projection, null, null, null);
        cur.moveToFirst();
        String preview = cur.getString(cur.getColumnIndex("preview"));
        String subject = cur.getString(cur.getColumnIndex("subject"));
    }

    /**
     *
     * @param context
     * @return

    private static Set<String> getAllAccountEmails(Context context) {
        //MessageProvider mp = new MessageProvider();
        //TODO where do we get the context from?
        final Account[] accounts = AccountManager.get(context).getAccounts();
        final Set<String> emailSet = new HashSet<String>();
        for (Account account : accounts) {
            if (Patterns.EMAIL_ADDRESS.matcher(account.name).matches())
                emailSet.add(account.name);
        }
        return emailSet;
    }*/
}
