package com.company.client;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import com.sun.xml.internal.messaging.saaj.packaging.mime.util.BASE64DecoderStream;
import com.sun.xml.internal.messaging.saaj.packaging.mime.util.BASE64EncoderStream;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

public class Encryption {
    private static Cipher ecipher;
    private static Cipher dcipher;

    private static Map<String, PublicKey> publicKeyMap;

    private static Encryption instance;

    private final static String KEYALGORITHM = "RSA";

    private static PublicKey publicKey;

    private Encryption() {
        try {
            // generate secret key using DES algorithm
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEYALGORITHM);
            keyGen.initialize(1024);
            KeyPair keyPair = keyGen.genKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();

            publicKey = keyPair.getPublic();

            ecipher = Cipher.getInstance(KEYALGORITHM);
            dcipher = Cipher.getInstance(KEYALGORITHM);

            // initialize the ciphers with the given key
            dcipher.init(Cipher.DECRYPT_MODE, privateKey);

            publicKeyMap = new HashMap<>();

        } catch (NoSuchAlgorithmException e) {
            System.out.println("No Such Algorithm:" + e.getMessage());
        } catch (NoSuchPaddingException e) {
            System.out.println("No Such Padding:" + e.getMessage());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    static Encryption getInstance() {
        if (instance == null) {
            instance = new Encryption();
        }
        return instance;
    }

    String encrypt(String userName, String message) {
        try {
            if (publicKeyMap.containsKey(userName)) {
                ecipher.init(Cipher.ENCRYPT_MODE, publicKeyMap.get(userName));
                byte[] utf8 = message.getBytes("UTF8");
                byte[] enc = ecipher.doFinal(utf8);
                enc = BASE64EncoderStream.encode(enc);
                return new String(enc);
            } else {
                return message;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    String decrypt(String message) {
        try {
            byte[] dec = BASE64DecoderStream.decode(message.getBytes());
            byte[] utf8 = dcipher.doFinal(dec);
            return new String(utf8, "UTF8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    void processPublicKeys(String userName, String publicKeyStr) {
        try {
            byte[] publicKeyByte = Base64.decode(publicKeyStr);

            // The bytes can be converted back to public and private key objects
            KeyFactory keyFactory = KeyFactory.getInstance(KEYALGORITHM);

            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyByte);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            addPublicKey(userName, publicKey);

        } catch (NoSuchAlgorithmException e) {
            System.out.println("No Such Algorithm:" + e.getMessage());
        } catch (InvalidKeySpecException e) {
            System.out.println("Invalid Key:" + e.getMessage());
        }
    }

    private void addPublicKey(String userName, PublicKey publicKey) {
        if (!publicKeyMap.containsKey(userName)) {
            publicKeyMap.put(userName, publicKey);
        }
    }

    String getPublicKey() {
        byte[] publicKeyByte = publicKey.getEncoded();
        return Base64.encode(publicKeyByte);
    }
}
