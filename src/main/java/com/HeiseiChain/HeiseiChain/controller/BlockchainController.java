package com.HeiseiChain.HeiseiChain.controller;

import com.HeiseiChain.HeiseiChain.model.*;
import com.HeiseiChain.HeiseiChain.service.BlockchainService;
import com.HeiseiChain.HeiseiChain.util.RSADecryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.json.JSONObject;


import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/blockchain")
public class BlockchainController {

    private final BlockchainService blockchainService;
    private static final String webapp_public =
            "-----BEGIN PUBLIC KEY-----\n" +
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApurNzyogbSS5Q8J7QE6a\n" +
                    "ujlqS71z6k3Z2MjRosL/lTN0l5De4T74R347nrBwaGy31MnddrjKaqEWJupsAF2b\n" +
                    "zb5BPxtt5G61MVWQ4Y3go7Qr+0BNGHP6hO4xsaxQB6alwI5ljzMI0FA3VmV2s69a\n" +
                    "KIIaycgIZKO+HMZuKnsbwRH8mc8T7e0lAiN7kpqtejJeNIadOQDvAPHZmRo0CuGh\n" +
                    "tew/bQcXRA+NN3p/mSCUfOQ8CxWi6f8mgHhBIVD5fw6zsKDNXbPMt+dW02M3LJXF\n" +
                    "UMRWt7EZO0wdRpbGbjJCpf7sT/oeW7BQRP89w3mRyyN97g7Bw/QKbkSoAKMQzxKc\n" +
                    "pwIDAQAB\n" +
                    "-----END PUBLIC KEY-----";

    private static final String java_private =
            "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC3poFp/NHPr8Xhu6Etd71p++Gh+0W/gz5NmSAVSzHF6+YOS6b1yXVLYPIWFDlBinAyE5nMrNSHtoDWU7S3ZBN9e+pXxejksplACPZ2AlscEohUGC/Tfd/jZ7jZlG2XD/cXvQnM8lIQ7H7p/VdfB9uZOERRIXmkm5wP+d3PBPn/vkrtr+y+NSp6oJPrVfsFUv+/VtQgrlWM/zCx/Cq8wu2OWTr1eT8a551y8oy97kqvXFaQ0s0Jdo1wYDjrT+04b4fIciJxsWs5Wv65nThu5YpgIBbY3YTonL7Cd4t1ZPss7aHRBHIyN49z8cyGYyy50+q/5UrR77rGel0Dm96s7OzbAgMBAAECggEAW2NXrgrZy4xwH7sTY0FdOBLYPpeAJ2OjitDitsXy8EdJ9Z4u6MuF+2s0PMl2jodZ+olndQMiRaCLb1w7JzF9Q2n4/RGmqbu4aDWtk57rgMY2IgbhMdDHK24PffKPMGeI8b6n1F1XekV9iGoB4u/Rlub5lBfg5QlseUTBXIXg9CJ/BFWldjI8WH2OHa9CdDdXTrnHvd1wtQzfyrTqC5spM0TeS6MS9kdcKw1ZhD7Q/wMSXoETvyNG8/lgzhzFtMwDhVWx4zjwdhJq9PRROnnqwaTH5OUlxe23dkJ8PzmGxV8jSt3jb7ubtagYlghvOgF2QmgNBc8EXoWxYtzXqZcLdQKBgQC6qoulkLWJaiVIRwhyzN/wIA5QdG+TMntQ2/EqSYYbOAUhb/ehkLjdaAKiAS5UiPmY3B2QRCRbWkH7LZdKcMwmxhpPvJDUddeM4xwzMs6Xy8bEn7urnsHrH/zXNO2+8FYCFxr8+j+eXDtyDeAGu7BSgRWk+FuepnOzAx5AKLJD9wKBgQD73TMbmd/k7Xj2mIFzA7xutaqHz/vP4PcMEZ32exXxflGLSbQ501b7BcOYwfeiTN1Z5U1hGQXVIrHbg2ij9e8udj0v54MlIHCT3BdCYsqsdAj9juz9vPu1zUjPoE0dEvA0Agq5BhlEsb7eVHFJxrrniGVr8YMbHLeFCXkS8c5dPQKBgARQqvfB16B8lq4MDlLxD4AyjYIonuetNBKTcwjCOFpquhuixfuzJ8Leg6kRE+waWoLBG+HY4WpHNN1EmYm7/8wpqjQLrOFsc/Yqzd0VIJd7u3WSJ7l46wyvaZ3j+FcAoUxdEl+kvVHA2hLx5SrirdnKaCCvKRcKzAPoX4umJTDTAoGARVNKeQNuvD2dOQsbPoQ9vp9kdAOMhVifx4Ol3i1dCd7CJTvBTtVcMLYSc56YQeU0XEUgemR/1X26RPizucW88yX6i5AG6hY2xowjHtPAg51gyCIqG2GESzNZIkU2VJVc5oPVXb5PADiIl/vYPv1jfs1tVTvh4XmTDhxgZhWGvuUCgYB7QVXmZjfU6Ypm6phq4vHGqwppp1wzkJ2rI4C2CNebjRHdc5lWOWik+aErmKCFz64HgLtUeV/vLWIPcPfK1Kq2dQ5QMO77Qk2sA7AOrbc+YE71XgS9Hj4JRYKORCSqur+AbcS3SQMv3LnAVOJRg546kST+z2HEwWaMWH0JfiDAVQ==\n" +
            "-----END PRIVATE KEY-----";

    public BlockchainController(BlockchainService blockchainService) {
        this.blockchainService = blockchainService; // Spring calls the constructor and injects this instance
    }

    // Load Private Key for Decryption
    private PrivateKey loadPrivateKey() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(java_private);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    // Load Web App's Public Key for Signature Verification
    private PublicKey loadWebAppPublicKey() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(webapp_public);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    }


    @GetMapping("/display")
    public String getBlockchain() {
        try {
            return blockchainService.getBlockchain();
        } catch (Exception e) {
            System.err.println("Error fetching blockchain: " + e.getMessage());
            return "Error";
        }
    }

    @GetMapping("/validate")
    public boolean isBlockchainValid() {
        return blockchainService.isChainValid();
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestBody SecureData requestData) {
        try{
            //username = RSADecryptionUtil.decryptData(username);
            //role = RSADecryptionUtil.decryptData(role);
            //System.out.println(username +" "+ role);
            // Verify Signature
            if (!verifySignature(requestData.encryptedData, requestData.signature)) {
                return "Invalid Signature!";
            }

            // Decrypt Data
            String decryptedJson = decryptData(requestData.encryptedData);
            JSONObject jsonObject = new JSONObject(decryptedJson);
            String username = jsonObject.getString("username");
            String role = jsonObject.getString("role");

                if (blockchainService.getWalletByUsername(username) != null)
                    return "Username already in use";
                    Wallet newWallet = blockchainService.createWallet(role);
                    // Check if the public key was generated properly
                    if (newWallet.publicKey == null) {
                        return "Error: Public key generation failed!";
                    }
                    blockchainService.registerUser(newWallet, username);

                return "User registered successfully!";

            } catch (Exception e) {
                return "Error registering user: " + e.getMessage();
            }
    }

    public static String decryptData(String encryptedData) throws Exception {
        String privateKeyPEM = new String(Files.readAllBytes(Paths.get("java_private.pem")));
        privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);

        //Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        //cipher.init(Cipher.DECRYPT_MODE, privateKey);

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256", "MGF1", new MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT
        );
        cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    public static boolean verifySignature(String data, String signature) throws Exception {
        String publicKeyPEM = new String(Files.readAllBytes(Paths.get("webapp_public.pem")));
        publicKeyPEM = publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyPEM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        return verifier.verify(signatureBytes);
    }

    @PostMapping("/generateReport")
    public ResponseEntity<byte[]> generateReport(
            @RequestParam("startDateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
            @RequestParam("endDateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime,
            Model model) {

        // Use the generateCSVReport method to create the CSV content
        String csvReport = blockchainService.getReport(startDateTime, endDateTime);

        // Convert the CSV string to bytes
        byte[] csvBytes = csvReport.getBytes();

        // Set response headers for downloading the file
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=blockchain_report.csv");

        // Return the response with the CSV file content
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }

    @PostMapping("/creation")
    public String creationTransaction(
            @RequestBody SecureData requestData) {
        try {
            /**
            senderUsername = RSADecryptionUtil.decryptData(senderUsername);
            recipientUsername = RSADecryptionUtil.decryptData(recipientUsername);
            value = RSADecryptionUtil.decryptData(value);
            transactionType = RSADecryptionUtil.decryptData(transactionType);
            commodity = RSADecryptionUtil.decryptData(commodity);

            // Convert value to float after decryption
            float decryptedValue = Float.parseFloat(value);
            **/

            if (!verifySignature(requestData.encryptedData, requestData.signature)) {
                return "Invalid Signature!";
            }

            // Decrypt Data
            String decryptedJson = decryptData(requestData.encryptedData);
            JSONObject jsonObject = new JSONObject(decryptedJson);
            String senderUsername = jsonObject.getString("senderUsername");
            String recipientUsername = jsonObject.getString("recipientUsername");
            String encryptedValue = jsonObject.getString("value");
            float value = Float.parseFloat(encryptedValue);
            String transactionType = jsonObject.getString("transactionType");
            String commodity = jsonObject.getString("commodity");

            // Step 1: Fetch the sender's public key
            PublicKey senderPublicKey = blockchainService.getPublicKeyByUsername(senderUsername);
            if (senderPublicKey == null) {
                return "Error: Sender username '" + senderUsername + "' is not registered!";
            }


            // Step 2: Fetch the recipient's public key
            PublicKey recipientPublicKey = blockchainService.getPublicKeyByUsername(recipientUsername);
            if (recipientPublicKey == null) {
                return "Error: Recipient username '" + recipientUsername + "' is not registered!";
            }

            // Step 3: Fetch the sender's wallet
            Wallet senderWallet = blockchainService.getWalletByUsername(senderUsername);
            if (senderWallet == null || senderWallet.privateKey == null) {
                return "Error: Could not retrieve wallet or private key for sender '" + senderUsername + "'!";
            }

            //System.out.println(senderWallet.privateKey);

            // Step 4: Create inputs (UTXOs) for the transaction
            ArrayList<TransactionInput> inputs = new ArrayList<>();

            // Skip UTXO checks if the metadata is "donation"
            if (!senderWallet.role.equals("donor")) {
                // Fetch UTXOs (unspent transaction outputs) for the sender
                List<UTXO> availableUTXOs = senderWallet.getUTXOs(commodity);
                if (availableUTXOs == null || availableUTXOs.isEmpty()) {
                    return "Error: No UTXOs available for sender '" + senderUsername + "'!";
                }

                // Prepare inputs for the transaction
                float totalInputValue = 0;
                for (UTXO utxo : availableUTXOs) {
                    inputs.add(new TransactionInput(utxo.getId()));
                    totalInputValue += utxo.getValue();
                    if (totalInputValue >= value) {
                        break;
                    }
                }

                // Check if the sender has sufficient funds
                if (totalInputValue < value) {
                    return String.format(
                            "Error: Insufficient funds for sender '%s'. Available: %.2f, Required: %.2f.",
                            senderUsername, totalInputValue, value
                    );
                }

            }

            // Step 5: Create the transaction
            String transactionID = blockchainService.createTransactionRequest(senderPublicKey, recipientPublicKey, commodity, value, inputs);
            return transactionID;
        } catch (Exception e) {
            return "Error creating transaction: " + e.getMessage();
        }
    }

    @PostMapping("/confirm")
    public String confirmTransaction(@RequestBody SecureData requestData) {
        Transaction transaction = null;
        String transactionID = null;

        try {
            if (!verifySignature(requestData.encryptedData, requestData.signature)) {
                return "Error: Invalid Signature!";
            }

            // Decrypt Data
            String decryptedJson = decryptData(requestData.encryptedData);
            JSONObject jsonObject = new JSONObject(decryptedJson);
            String senderUsername = jsonObject.getString("senderUsername");
            transactionID = jsonObject.getString("transactionID");

            // Retrieve transaction inputs
            Object[] transactionIN = blockchainService.retreiveTransactionInputs(transactionID);
            if (transactionIN == null) {
                return "Error: Transaction not found for ID " + transactionID;
            }

            ArrayList<TransactionInput> inputs = new ArrayList<>();
            Wallet senderWallet = blockchainService.getWalletByUsername(senderUsername);

            if (senderWallet == null) {
                return "Error: Sender wallet not found!";
            }

            // Skip UTXO checks if the metadata is "donation"
            if (!senderWallet.role.equals("donor")) {
                List<UTXO> availableUTXOs = senderWallet.getUTXOs((String) transactionIN[4]);

                if (availableUTXOs == null || availableUTXOs.isEmpty()) {
                    return "Error: No UTXOs available for sender '" + senderUsername + "'!";
                }

                float value = (Float) transactionIN[3];
                float totalInputValue = 0;

                for (UTXO utxo : availableUTXOs) {
                    inputs.add(new TransactionInput(utxo.getId()));
                    totalInputValue += utxo.getValue();
                    if (totalInputValue >= value) {
                        break;
                    }
                }

                if (totalInputValue < value) {
                    return String.format(
                            "Error: Insufficient funds for sender '%s'. Available: %.2f, Required: %.2f.",
                            senderUsername, totalInputValue, value
                    );
                }
            }

            // Create transaction object
            transaction = new Transaction(
                    (PublicKey) transactionIN[0],
                    (PublicKey) transactionIN[1],
                    (String) transactionIN[2],
                    (Float) transactionIN[3],
                    (String) transactionIN[4],
                    inputs,
                    (Long) transactionIN[6]
            );

            // Generate signature
            transaction.generateSignature(senderWallet.privateKey);

            // Process transaction
            Map<PublicKey, Float> donor = transaction.processTransaction();
            if (donor != null) {
                String endPoint = blockchainService.addTransaction(transaction, donor);
                return (endPoint == null)
                        ? "Transaction created successfully! Transaction ID: " + transaction.getTransactionId()
                        : "ACK " + endPoint;
            } else {
                throw new Exception("Transaction failed during processing.");
            }

        } catch (Exception e) {
            if (transaction != null && transactionID != null) {
                blockchainService.reinsertTransaction(transactionID, new Object[]{
                        transaction.sender,
                        transaction.recipient,
                        transaction.transactionId,
                        transaction.value,
                        transaction.metadata,
                        transaction.inputs
                });
            }
            return "Error creating transaction: " + e.getMessage();
        }
    }


    @GetMapping("/displayWallet")
    public String displayWallet(){
        return blockchainService.displayWallets();
    }

    @GetMapping("/donationPercentage")
    public float displayPercentage(){
        return blockchainService.displayPercentage();
    }


    // DTO Class for Receiving Data
    static class SecureData {
        public String encryptedData;
        public String signature;
    }


}
