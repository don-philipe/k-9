
package de.tud.smime.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;

public class SmimeApi {

    public static final String TAG = "Smime API";

    // this aidl file doesn't exist at the moment
    public static final String SERVICE_INTENT = "de.tud.smime.ISmimeService";

    public static final int API_VERSION = 1;

    // TODO whats that?
    public static final String ACTION_SIGN = "de.tud.smime.action.SIGN";
    public static final String ACTION_CLEARTEXT_SIGN = "de.tud.smime.action.CLEARTEXT_SIGN";
    public static final String ACTION_DETACHED_SIGN = "de.tud.smime.action.DETACHED_SIGN";
    public static final String ACTION_ENCRYPT = "de.tud.smime.action.ENCRYPT";
    public static final String ACTION_SIGN_AND_ENCRYPT = "de.tud.smime.action.SIGN_AND_ENCRYPT";
    public static final String ACTION_DECRYPT_VERIFY = "de.tud.smime.action.DECRYPT_VERIFY";
    public static final String ACTION_DECRYPT_METADATA = "de.tud.smime.action.DECRYPT_METADATA";
    public static final String ACTION_GET_KEY_IDS = "de.tud.smime.action.GET_KEY_IDS";
    public static final String ACTION_GET_KEY = "de.tud.smime.action.GET_KEY";

    /* Intent extras */
    public static final String EXTRA_API_VERSION = "api_version";

    public static final String EXTRA_ACCOUNT_NAME = "account_name";

    // ACTION_DETACHED_SIGN, ENCRYPT, SIGN_AND_ENCRYPT, DECRYPT_VERIFY
    // request ASCII Armor for output
    // OpenPGP Radix-64, 33 percent overhead compared to binary, see http://tools.ietf.org/html/rfc4880#page-53)
    public static final String EXTRA_REQUEST_ASCII_ARMOR = "ascii_armor";

    // ACTION_DETACHED_SIGN
    public static final String RESULT_DETACHED_SIGNATURE = "detached_signature";

    // ENCRYPT, SIGN_AND_ENCRYPT
    public static final String EXTRA_USER_IDS = "user_ids";
    public static final String EXTRA_KEY_IDS = "key_ids";
    // optional extras:
    public static final String EXTRA_PASSPHRASE = "passphrase";
    public static final String EXTRA_ORIGINAL_FILENAME = "original_filename";

    // internal NFC states
    public static final String EXTRA_NFC_SIGNED_HASH = "nfc_signed_hash";
    public static final String EXTRA_NFC_SIG_CREATION_TIMESTAMP = "nfc_sig_creation_timestamp";
    public static final String EXTRA_NFC_DECRYPTED_SESSION_KEY = "nfc_decrypted_session_key";

    // GET_KEY
    public static final String EXTRA_KEY_ID = "key_id";
    public static final String RESULT_KEY_IDS = "key_ids";

    /* Service Intent returns */
    public static final String RESULT_CODE = "result_code";

    // get actual error object from RESULT_ERROR
    public static final int RESULT_CODE_ERROR = 0;
    // success!
    public static final int RESULT_CODE_SUCCESS = 1;
    // get PendingIntent from RESULT_INTENT, start PendingIntent with startIntentSenderForResult,
    // and execute service method again in onActivityResult
    public static final int RESULT_CODE_USER_INTERACTION_REQUIRED = 2;

    public static final String RESULT_ERROR = "error";
    public static final String RESULT_INTENT = "intent";

    // DECRYPT_VERIFY
    public static final String EXTRA_DETACHED_SIGNATURE = "detached_signature";

    public static final String RESULT_SIGNATURE = "signature";
    public static final String RESULT_METADATA = "metadata";
    // This will be the charset which was specified in the headers of ascii armored input, if any
    public static final String RESULT_CHARSET = "charset";

    ISmimeService mService;
    Context mContext;

    public SmimeApi(Context context, ISmimeService service) {
        this.mContext = context;
        this.mService = service;
    }

    public interface ISmimeCallback {
        void onReturn(final Intent result);
    }

    private class SmimeAsyncTask extends AsyncTask<Void, Integer, Intent> {
        Intent data;
        InputStream is;
        OutputStream os;
        ISmimeCallback callback;

        private SmimeAsyncTask(Intent data, InputStream is, OutputStream os, ISmimeCallback callback) {
            this.data = data;
            this.is = is;
            this.os = os;
            this.callback = callback;
        }

        @Override
        protected Intent doInBackground(Void... unused) {
            return executeApi(data, is, os);
        }

        protected void onPostExecute(Intent result) {
            callback.onReturn(result);
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void executeApiAsync(Intent data, InputStream is, OutputStream os, ISmimeCallback callback) {
        SmimeAsyncTask task = new SmimeAsyncTask(data, is, os, callback);

        // don't serialize async tasks!
        // http://commonsware.com/blog/2012/04/20/asynctask-threading-regression-confirmed.html
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        } else {
            task.execute((Void[]) null);
        }
    }

    public Intent executeApi(Intent data, InputStream is, OutputStream os) {
        try {
            data.putExtra(EXTRA_API_VERSION, SmimeApi.API_VERSION);

            Intent result;

            // pipe the input and output
            ParcelFileDescriptor input = null;
            if (is != null) {
                input = ParcelFileDescriptorUtil.pipeFrom(is,
                        new ParcelFileDescriptorUtil.IThreadListener() {

                            @Override
                            public void onThreadFinished(Thread thread) {
                                //Log.d(OpenPgpApi.TAG, "Copy to service finished");
                            }
                        }
                );
            }
            ParcelFileDescriptor output = null;
            if (os != null) {
                output = ParcelFileDescriptorUtil.pipeTo(os,
                        new ParcelFileDescriptorUtil.IThreadListener() {

                            @Override
                            public void onThreadFinished(Thread thread) {
                                //Log.d(OpenPgpApi.TAG, "Service finished writing!");
                            }
                        }
                );
            }

            // blocks until result is ready
            result = mService.execute(data, input, output);
            // close() is required to halt the TransferThread
            if (output != null) {
                output.close();
            }
            // TODO: close input?

            // set class loader to current context to allow unparcelling
            // of OpenPgpError and OpenPgpSignatureResult
            // http://stackoverflow.com/a/3806769
            result.setExtrasClassLoader(mContext.getClassLoader());

            return result;
        } catch (Exception e) {
            Log.e(SmimeApi.TAG, "Exception in executeApi call", e);
            Intent result = new Intent();
            result.putExtra(RESULT_CODE, RESULT_CODE_ERROR);
            result.putExtra(RESULT_ERROR,
                    new SmimeError(SmimeError.CLIENT_SIDE_ERROR, e.getMessage()));
            return result;
        }
    }

}
