package de.tud.smime.cert;

import android.net.Uri;

import com.fsck.k9.Account;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings;
import com.fsck.k9.mail.ssl.LocalKeyStore;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Created by don on 15.02.15.
 */
public class CertStoreHelper {

    /**
     *
     */
    public void addCertificate(AccountSetupCheckSettings.CheckDirection direction,
                               Account account, X509Certificate certificate) throws CertificateException {
        Uri uri;
        if (direction == AccountSetupCheckSettings.CheckDirection.INCOMING) {
            uri = Uri.parse(account.getStoreUri());
        } else {
            uri = Uri.parse(account.getTransportUri());
        }
        LocalKeyStore localKeyStore = LocalKeyStore.getInstance();
        localKeyStore.addCertificate(uri.getHost(), uri.getPort(), certificate);
    }

    /**
     *
     */
    public void deleteCertificate(String newHost, int newPort,
                                  AccountSetupCheckSettings.CheckDirection direction, Account account) {
        Uri uri;
        if (direction == AccountSetupCheckSettings.CheckDirection.INCOMING) {
            uri = Uri.parse(account.getStoreUri());
        } else {
            uri = Uri.parse(account.getTransportUri());
        }
        String oldHost = uri.getHost();
        int oldPort = uri.getPort();
        if (oldPort == -1) {
            // This occurs when a new account is created
            return;
        }
        if (!newHost.equals(oldHost) || newPort != oldPort) {
            LocalKeyStore localKeyStore = LocalKeyStore.getInstance();
            localKeyStore.deleteCertificate(oldHost, oldPort);
        }
    }

    /**
     * Deletes all certificates in store.
     */
    public void deleteAllCertificates(Account account) {
        LocalKeyStore localKeyStore = LocalKeyStore.getInstance();

        String storeUri = account.getStoreUri();
        if (storeUri != null) {
            Uri uri = Uri.parse(storeUri);
            localKeyStore.deleteCertificate(uri.getHost(), uri.getPort());
        }
        String transportUri = account.getTransportUri();
        if (transportUri != null) {
            Uri uri = Uri.parse(transportUri);
            localKeyStore.deleteCertificate(uri.getHost(), uri.getPort());
        }
    }
}
