package de.tud.smime.mail;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
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
    public static boolean readMail(MimeMessage message) {
        try {
            MimeMessage msg = new MimeMessage(message);
            boolean sig_correct = false;
            Object cont = msg.getContent();
            if (cont instanceof String) {
                System.out.println((String)cont);
            }
            else if (cont instanceof Multipart) {
                Multipart mp = (Multipart) cont;
                int count = mp.getCount();
                for (int i = 0; i < count; i++) {
                    BodyPart m = mp.getBodyPart(i);
                    String conttype = m.getContentType();
                    Object part = m.getContent();
                }
            }
            if(msg.isMimeType("multipart/signed") || msg.isMimeType("application/pkcs7-mime") || msg.isMimeType("application/x-pkcs7-mime"))
                sig_correct = readSignedMail(msg);
            if(msg.isMimeType("application/pkcs7-mime") && sig_correct)
                //TODO get the right keystore uri
                readEncryptedMail(msg, "keystore_uri");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     *
     * @param msg the message to decrypt
     * @param pkcs12Keystore URI to the keystore
     */
    private static void readEncryptedMail(Message msg, String pkcs12Keystore) {
        try {
            /*// Open the key store
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
            System.out.println(res.getContent());*/
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
            if (msg.isMimeType("multipart/signed") || msg.isMimeType("multipart/mixed")) {
                SMIMESigned s = new SMIMESigned((MimeMultipart)msg.getContent());

                // extract the content
                MimeBodyPart content = s.getContent();
                //System.out.println("Content:");
                Object cont = content.getContent();

                if (cont instanceof String) {
                    System.out.println((String)cont);
                }
                else if (cont instanceof Multipart) {
                    Multipart mp = (Multipart)cont;
                    int count = mp.getCount();
                    for (int i = 0; i < count; i++) {
                        BodyPart m = mp.getBodyPart(i);
                        Object part = m.getContent();

                        System.out.println("Part " + i);
                        System.out.println("---------------------------");

                        if (part instanceof String) {
                            String st = (String) part;
                            System.out.println((String)part);
                        }
                        else {
                            System.out.println("can't print...");
                        }
                    }
                }

                System.out.println("Status:");

                //TODO
                // verify(s);
            }
            else if (msg.isMimeType("application/pkcs7-mime") || msg.isMimeType("application/x-pkcs7-mime")) {
                // in this case the content is wrapped in the signature block.
                SMIMESigned s = new SMIMESigned(msg);

                // extract the content
                MimeBodyPart content = s.getContent();
                System.out.println("Content:");
                Object cont = content.getContent();

                if (cont instanceof String) {
                    System.out.println((String)cont);
                }

                System.out.println("Status:");
                //TODO
                // verify(s);
            }
            else {
                System.err.println("Not a signed message!");
            }
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (CMSException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SMIMEException e) {
            e.printStackTrace();
        }
        return correct_sig;
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
