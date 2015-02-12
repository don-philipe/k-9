package de.tud.smime.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.util.Patterns;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by don on 12.02.15.
 */
public class EmailHelper {

    /**
     *
     * @param context
     * @return
     */
    private static Set<String> getAllAccountEmails(Context context) {
        //TODO where do we get the context from?
        final Account[] accounts = AccountManager.get(context).getAccounts();
        final Set<String> emailSet = new HashSet<>();
        for (Account account : accounts) {
            if (Patterns.EMAIL_ADDRESS.matcher(account.name).matches())
                emailSet.add(account.name);
        }
        return emailSet;
    }
}
