package com.andrejhucko.andrej.backend.utility;

import java.io.*;
import java.util.*;
import javax.crypto.*;
import java.security.*;
import android.util.Log;
import java.math.BigInteger;
import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import javax.security.auth.x500.X500Principal;

public class CryptoHandler {

    private static final String TAG = "CryptoHandler";
    private static final String ALIAS = "andrej";

    private Context context;

    CryptoHandler(Context context) {
        this.context = context;
    }

    String encrypt(String data) {
        if (data == null) return null;

        KeyStore.PrivateKeyEntry privateKeyEntry = getKeyStoreEntry();
        if (privateKeyEntry == null) return null;

        PublicKey publicKey = privateKeyEntry.getCertificate().getPublicKey();

        try {
            Cipher inCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            inCipher.init(Cipher.ENCRYPT_MODE, publicKey);

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(byteStream, inCipher);
            cipherOutputStream.write(data.getBytes("UTF-8"));
            cipherOutputStream.close();

            // Custom implementation of representing the array of bytes
            byte[] encrypted = byteStream.toByteArray();
            int encryptedLength = encrypted.length;

            StringBuilder builder = new StringBuilder();
            builder.append(encryptedLength).append(";");
            for(byte b : encrypted) {
                builder.append(b).append(",");
            }

            return builder.toString();
        }
        catch (Exception e) {
            Log.wtf(TAG, "Exception: encrypt");
            return null;
        }

    }

    String decrypt(String data) {
        if (data == null) return null;

        KeyStore.PrivateKeyEntry privateKeyEntry = getKeyStoreEntry();
        if (privateKeyEntry == null)
            return null;

        PrivateKey privateKey = privateKeyEntry.getPrivateKey();

        try {

            Cipher outCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            outCipher.init(Cipher.DECRYPT_MODE, privateKey);

            int length = Integer.valueOf(data.substring(0, data.indexOf(";")));
            byte[] byteData = new byte[length];
            int dataIndex = data.indexOf(";") + 1;
            int byteIndex = 0;

            while (data.length() > dataIndex) {
                int lastCommaPos = data.indexOf(",", dataIndex);
                byte value = Byte.valueOf(data.substring(dataIndex, lastCommaPos));
                dataIndex = lastCommaPos + 1;
                byteData[byteIndex] = value;
                byteIndex++;
            }

            ByteArrayInputStream inputStream = new ByteArrayInputStream(byteData);
            CipherInputStream cipherInputStream = new CipherInputStream(inputStream, outCipher);
            byte[] roundTrippedBytes = new byte[1024];

            int index = 0;
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                roundTrippedBytes[index] = (byte) nextByte;
                index++;
            }

            return new String(roundTrippedBytes, 0, index, "UTF-8");

        }
        catch (Exception e) {
            Log.wtf(TAG, "Exception: decrypt");
            e.printStackTrace();
            return null;
        }

    }

    private KeyStore.PrivateKeyEntry getKeyStoreEntry() {

        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            if (!keyStore.containsAlias(ALIAS)) {
                generateNewKeys(); // Create the keys if necessary
            }
            return (KeyStore.PrivateKeyEntry) keyStore.getEntry(ALIAS, null);

        }
        catch (Exception e) {
            Log.wtf(TAG, "Exception from: getKeyStoreEntry");
            return null;
        }

    }

    private void generateNewKeys() {

        try {
            Calendar notBefore = Calendar.getInstance();
            Calendar notAfter = Calendar.getInstance();
            notAfter.add(Calendar.YEAR, 1);

            KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                    .setSubject(new X500Principal("CN=andrej"))
                    .setStartDate(notBefore.getTime())
                    .setEndDate(notAfter.getTime())
                    .setSerialNumber(BigInteger.ONE)
                    .setAlias("andrej")
                    .setKeySize(2048)
                    .build();

            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
            generator.initialize(spec);

            /* KeyPair keyPair = */
            generator.generateKeyPair();
        }
        catch (Exception e) {
            Log.wtf(TAG, "EXCEPTION :: " + e.getClass().getSimpleName());
            Log.wtf(TAG, Arrays.toString(e.getStackTrace()));
        }

    }

    static String md5(String input) {

        if (input == null) return null;
        try {
            StringBuilder builder = new StringBuilder();
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input.getBytes());
            byte[] digest = md.digest();
            for (byte b : digest) {
                String hex = Integer.toHexString(b);
                if (hex.length() > 2) hex = hex.substring(hex.length() - 2);
                if (hex.length() == 1) hex = "0" + hex;
                builder.append(hex);
            }
            return builder.toString();
        }
        catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

}
