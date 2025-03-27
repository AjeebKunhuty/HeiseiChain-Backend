package com.HeiseiChain.HeiseiChain.util;

import javax.crypto.Cipher;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class RSADecryptionUtil {

    private static final String PRIVATE_KEY_PATH = "src/main/resources/security/private.key";
    private static PrivateKey privateKey;

    static {
        try {
            InputStream keyStream = RSADecryptionUtil.class.getResourceAsStream(PRIVATE_KEY_PATH);
            if (keyStream == null) {
                throw new RuntimeException("Private key file not found: " + PRIVATE_KEY_PATH);
            }
            byte[] keyBytes = Files.readAllBytes(Paths.get(PRIVATE_KEY_PATH));
            String privateKeyPEM = new String(keyBytes)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decodedKey = Base64.getDecoder().decode(privateKeyPEM);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Error loading RSA private key", e);
        }
    }

    public static String decryptData(String encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes);
    }
}
