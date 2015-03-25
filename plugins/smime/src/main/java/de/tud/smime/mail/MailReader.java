package de.tud.smime.mail;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Created by don on 01.03.15.
 */
public class MailReader {
    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;

    /**
     *
     */
    public static boolean readMail(InputStream message) {
        Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);
        try {
            MimeMessage mm = new MimeMessage(session, message);
            if(mm.isMimeType("application/pkcs7-signature") || mm.isMimeType("multipart/signed"))
                return readSignedMail(mm);
//            if(message.hasAttachments()) {
//                LinkedList<String> mimtypes = new LinkedList<String>();
//                if (message.hasAttachments()) {
//                    for (int i = 0; i < message.getCount(); i++) {
//                        try {
//                            mimtypes.add(message.getBodyPart(i).getMimeType());
//                        } catch (MessagingException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//                if(mimtypes.contains("application/pkcs7-signature") || mimtypes.contains("multipart/signed")) {
//                    InputStream is = new ByteArrayInputStream(mData.getBytes(Charset.forName("UTF-8")));
//
//                }
//            }
        } catch (MessagingException e) {
            e.printStackTrace();
        }
//        try {
//            Properties props = System.getProperties();
//            Session session = Session.getDefaultInstance(props, null);
//            MimeMessage msg = new MimeMessage(session, fis);
//            boolean sig_correct = false;
//            if(msg.isMimeType("multipart/signed") || msg.isMimeType("application/pkcs7-mime") || msg.isMimeType("application/x-pkcs7-mime"))
//                return readSignedMail(msg);
//            if(msg.isMimeType("application/pkcs7-mime") && sig_correct)
//                //TODO get the right keystore uri
//                readEncryptedMail(msg, "keystore_uri");
//        }
//        catch (Exception e) {
//            //TODO
//        }
        return false;
    }

    /**
     *
     * @param msg the message to decrypt
     * @param pkcs12Keystore URI to the keystore
     */
    private static void readEncryptedMail(MimeMessage msg, String pkcs12Keystore) {
        try {
            // Open the key store
            KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
            //TODO of course a fixed password is not the final solution
            ks.load(new FileInputStream(pkcs12Keystore), "passwd".toCharArray());
            Enumeration e = ks.aliases();
            String keyAlias = null;
            while (e.hasMoreElements()) {
                String alias = (String) e.nextElement();
                if (ks.isKeyEntry(alias))
                    keyAlias = alias;
            }
            if (keyAlias == null) {
                //TODO
                System.err.println("can't find a private key!");
                System.exit(0);
            }
            // find the certificate for the private key and generate a
            // suitable recipient identifier.
            X509Certificate cert = (X509Certificate) ks.getCertificate(keyAlias);
            RecipientId recId = new JceKeyTransRecipientId(cert);

            SMIMEEnveloped m = new SMIMEEnveloped(msg);
            RecipientInformationStore recipients = m.getRecipientInfos();
            RecipientInformation recipient = recipients.get(recId);
            MimeBodyPart res = SMIMEUtil.toMimeBodyPart(recipient.getContent(new JceKeyTransEnvelopedRecipient((PrivateKey) ks.getKey(keyAlias, null)).setProvider("BC")));
            //TODO
            System.out.println("Message Contents");
            System.out.println("----------------");
            System.out.println(res.getContent());
        }
        catch (Exception e) {
            // TODO
        }
    }

    /**
     *
     * @param msg
     * @return true if the certificate was correct an no other errors happend.
     * @throws MessagingException
     */
    private static boolean readSignedMail(MimeMessage msg) {
        boolean correct_sig = false;
        try {
            if (msg.isMimeType("multipart/signed")) {
                SMIMESigned s = new SMIMESigned((MimeMultipart) msg.getContent());

                // extract the content
                MimeBodyPart content = s.getContent();
                System.out.println("Content:");
                Object cont = content.getContent();

                if (cont instanceof String)
                    System.out.println((String) cont);
                else if (cont instanceof Multipart) {
                    Multipart mp = (Multipart) cont;
                    int count = mp.getCount();
                    for (int i = 0; i < count; i++) {
                        BodyPart m = mp.getBodyPart(i);
                        Object part = m.getContent();

                        //TODO
                        System.out.println("Part " + i);
                        System.out.println("---------------------------");

                        if (part instanceof String)
                            System.out.println((String) part);
                        else
                            System.out.println("can't print...");
                    }
                }

                System.out.println("Status:");
                correct_sig = verifySignedMailWithCert(s);
            }
            // signed encrypted mails:
            else if (msg.isMimeType("application/pkcs7-mime") || msg.isMimeType("application/x-pkcs7-mime")) {
                // in this case the content is wrapped in the signature block.
                SMIMESigned s = new SMIMESigned(msg);

                // extract the content
                MimeBodyPart content = s.getContent();
                System.out.println("Content:");
                Object cont = content.getContent();

                if (cont instanceof String)
                    System.out.println((String) cont);

                System.out.println("Status:");

                correct_sig = verifySignedMailWithCert(s);
            }
            else
                return false;
        }
        catch(Exception e) {
            //TODO
        }
        finally {
            return correct_sig;
        }
    }

    /**
     * Verifies mail which has its certificate attached.
     * @param s
     * @return
     * @throws Exception
     */
    private static boolean verifySignedMailWithCert(SMIMESigned s) throws CertificateException, OperatorCreationException, CMSException {
        // extract the information to verify the signatures.
        // certificates and CRLs passed in the signature
        Store certs = s.getCertificates();
        // SignerInfo blocks which contain the signatures
        SignerInformationStore signers = s.getSignerInfos();

        Collection c = signers.getSigners();
        Iterator it = c.iterator();

        // check each signer
        boolean sig_correct_flag = false;
        while (it.hasNext()) {
            SignerInformation signer = (SignerInformation)it.next();
            Collection certCollection = certs.getMatches(signer.getSID());

            Iterator certIt = certCollection.iterator();
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC).getCertificate((X509CertificateHolder)certIt.next());

            // verify that the sig is correct and that it was generated
            // when the certificate was current
            if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(cert)))
                sig_correct_flag = true;
            else {
                sig_correct_flag = false;
                break;
            }
        }
        return sig_correct_flag;
    }
}
