package com.HeiseiChain.HeiseiChain.controller;

import com.HeiseiChain.HeiseiChain.model.*;
import com.HeiseiChain.HeiseiChain.service.BlockchainService;
import com.HeiseiChain.HeiseiChain.util.RSADecryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/blockchain")
public class BlockchainController {

    private final BlockchainService blockchainService;
    public BlockchainController(BlockchainService blockchainService) {
        this.blockchainService = blockchainService; // Spring calls the constructor and injects this instance
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
            @RequestParam String username,
            @RequestParam String role) {
        try{
            //username = RSADecryptionUtil.decryptData(username);
            //role = RSADecryptionUtil.decryptData(role);
            //System.out.println(username +" "+ role);
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
            @RequestParam String senderUsername,
            @RequestParam String recipientUsername,
            @RequestParam Float value,
            @RequestParam String transactionType,
            @RequestParam String commodity) {
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
    public String confirmTransaction(
            @RequestParam String senderUsername,
            @RequestParam String transactionID) {

        Object[] transactionIN = blockchainService.retreiveTransactionInputs(transactionID);
        ArrayList<TransactionInput> inputs = new ArrayList<>();

        Wallet senderWallet = blockchainService.getWalletByUsername(senderUsername);
        // Skip UTXO checks if the metadata is "donation"
        if (!senderWallet.role.equals("donor")) {
            // Fetch UTXOs (unspent transaction outputs) for the sender
            List<UTXO> availableUTXOs = senderWallet.getUTXOs((String) transactionIN[4]);
            if (availableUTXOs == null || availableUTXOs.isEmpty()) {
                return "Error: No UTXOs available for sender '" + senderUsername + "'!";
            }

            float value = (Float) transactionIN[3];
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
        Transaction transaction = new Transaction((PublicKey) transactionIN[0],
                (PublicKey) transactionIN[1],
                (String) transactionIN[2],
                (Float) transactionIN[3],
                (String) transactionIN[4],
                inputs,
                (Long) transactionIN[6]);

        try{

            // Step 6: Generate the transaction's signature using the sender's private key
            transaction.generateSignature(senderWallet.privateKey);

            // Step 7: Process the transaction
            Map<PublicKey,Float> donor = transaction.processTransaction();
            //System.out.println(success);
            if (donor != null) {
                String endPoint = blockchainService.addTransaction(transaction,donor);
                if(endPoint == null)
                    return "Transaction created successfully! Transaction ID: " + transaction.getTransactionId();
                else
                    return "ACK "+endPoint;
            } else {
                throw new Exception("Transaction failed during processing");
            }
        } catch (Exception e) {
            if(transaction != null)
                blockchainService.reinsertTransaction(transactionID, new Object[]{transaction.sender, transaction.recipient, transaction.transactionId, transaction.value, transaction.metadata,transaction.inputs});
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
}
