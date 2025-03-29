package com.HeiseiChain.HeiseiChain.util;

import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.core.io.ClassPathResource;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import javax.crypto.Cipher;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class RSADecryptionUtil {
        private static final String PRIVATE_KEY_PATH = "src/main/resources/keys/private.key"; // Set the correct path

        private static PrivateKey loadPrivateKey(String filePath) throws Exception {
            String keyContent = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", ""); // Remove spaces and new lines

            byte[] decodedKey = Base64.getDecoder().decode(keyContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        }

        public static String decryptData(String encryptedData) throws Exception {
            PrivateKey privateKey = loadPrivateKey(PRIVATE_KEY_PATH);

            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        }
    }