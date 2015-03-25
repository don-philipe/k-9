
package com.fsck.k9.crypto;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Multipart;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeUtility;


public class CryptoHelper {

    public static final Pattern PGP_MESSAGE =
            Pattern.compile(".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*",
                    Pattern.DOTALL);

    public static final Pattern PGP_SIGNED_MESSAGE =
            Pattern.compile(
                    ".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*",
                    Pattern.DOTALL);

    public CryptoHelper() {
        super();
    }

    /**
     * TODO: use new parseMessage() from PgpUtils to actually parse!
     * @param message
     * @return
     */
    public boolean isEncrypted(Message message) {
        String data = null;

        if(message.hasAttachments())    // first try attachments
        {
            LinkedList<String> mimtypes = new LinkedList<String>();
            if (message.hasAttachments()) {
                Multipart mp = (Multipart) message.getBody();
                for (int i = 0; i < mp.getCount(); i++) {
                    try {
                        mimtypes.add(mp.getBodyPart(i).getMimeType());
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                }
            }
            if(mimtypes.contains("application/pkcs7-signature"))
                return true;
        }
        try {
            Part part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
            if (part == null) {
                part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            }
            if (part != null) {
                data = MessageExtractor.getTextFromPart(part);
            }
        } catch (MessagingException e) {
            // guess not...
            // TODO: maybe log this?
        }

        if (data == null) {
            return false;
        }

        Matcher matcher = PGP_MESSAGE.matcher(data);
        return matcher.matches();
    }

    public boolean isSigned(Message message) {
        String data = null;

        if(message.hasAttachments())    // first try attachments
        {
            LinkedList<String> mimtypes = new LinkedList<String>();
            if (message.hasAttachments()) {
                Multipart mp = (Multipart) message.getBody();
                for (int i = 0; i < mp.getCount(); i++) {
                    try {
                        mimtypes.add(mp.getBodyPart(i).getMimeType());
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                }
            }
            if(mimtypes.contains("application/pkcs7-signature") || mimtypes.contains("multipart/signed"))
                return true;
        }
        try {
            Part part = MimeUtility.findFirstPartByMimeType(message, "text/plain");
            if (part == null) {
                part = MimeUtility.findFirstPartByMimeType(message, "text/html");
            }
            if (part != null) {
                data = MessageExtractor.getTextFromPart(part);
            }
        } catch (MessagingException e) {
            // guess not...
            // TODO: maybe log this?
        }

        if (data == null) {
            return false;
        }

        Matcher matcher = PGP_SIGNED_MESSAGE.matcher(data);
        return matcher.matches();
    }
}
