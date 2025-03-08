package com.HeiseiChain.HeiseiChain.service;

import com.HeiseiChain.HeiseiChain.model.*;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class BlockchainService {

    private final Blockchain blockchain;
    private Map<String, Wallet> walletDatabase = new HashMap<>();
    private final Map<String, Transaction> pendingTransactions = new HashMap<>();

    public BlockchainService() {
        this.blockchain = new Blockchain();
    }

    public String getBlockchain() {
        return blockchain.displayHTML(walletDatabase);
    }

    public String getReport(LocalDateTime startDateTime, LocalDateTime endDateTime) { return blockchain.generateCSVReport(walletDatabase,pendingTransactions,startDateTime,endDateTime);}

    public void addTransaction(Transaction transaction) {
        blockchain.addTransaction(transaction);
    }

    public boolean isChainValid() {
        return blockchain.isChainValid();
    }

    public Wallet createWallet(String role) {
        Wallet wallet = new Wallet(role);  // Create a wallet with the specified role
        return wallet;
    }

    // Registers the user by adding a registration transaction
    public void registerUser(Wallet wallet, String username) {
        RegistrationTransaction registrationTransaction = new RegistrationTransaction(wallet.publicKey, username);
        blockchain.addTransaction(registrationTransaction);
        walletDatabase.put(username, wallet);
    }

    public PublicKey getPublicKeyByUsername(String username) {
        for (Block block : blockchain.getChain()) {
            for (Transaction transaction : block.getTransactions()) {
                if (transaction instanceof RegistrationTransaction) {
                    RegistrationTransaction regTransaction = (RegistrationTransaction) transaction;
                    if (regTransaction.getMetadata().contains(username)) {
                        return regTransaction.getSender(); // Registered public key
                    }
                }
            }
        }
        return null; // User not found
    }

    public Wallet getWalletByUsername(String username) {
        return walletDatabase.get(username); // Retrieve wallet by username
    }


    public String createTransactionRequest(PublicKey senderId, PublicKey recipientId, String commodity, float quantity, ArrayList<TransactionInput>inputs) {
        String transactionId = UUID.randomUUID().toString(); // Generate unique ID
        Transaction transaction = new Transaction(senderId, recipientId, transactionId, quantity, commodity,inputs);
        pendingTransactions.put(transactionId, transaction);
        return transactionId;
    }

    public Transaction confirmTransaction(String transactionId) {
        if (pendingTransactions.containsKey(transactionId)) {
            return pendingTransactions.remove(transactionId);
        }
        return null;
    }

    public String displayWallets() {
        StringBuilder html = new StringBuilder();

        html.append("<html><head><title>Wallet Commodities</title>")
                .append("<style>")
                .append("body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }")
                .append(".container { width: 60%; margin: auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); }")
                .append("h2 { text-align: center; }")
                .append(".wallet { padding: 10px; border-bottom: 1px solid #ddd; }")
                .append(".wallet:last-child { border-bottom: none; }")
                .append("</style>")
                .append("</head><body>")
                .append("<div class='container'><h2>Wallet Commodity Holdings</h2>");

        for (String w : walletDatabase.keySet()) {
            if (walletDatabase.get(w).role.equals("volunteer")) {
                html.append("<div class='wallet'><strong>Wallet:</strong> ")
                        .append(w) //Wallet username
                        .append("<br><strong>Commodities:</strong> <ul>");

                // Append each commodity and its quantity
                for (Map.Entry<String, Float> entry : walletDatabase.get(w).getCommodityQuantities().entrySet()) {
                    html.append("<li>")
                            .append(entry.getKey()) // Commodity Name
                            .append(": ")
                            .append(entry.getValue()) // Quantity
                            .append("</li>");
                }

                html.append("</ul></div>");
            }
        }

        html.append("</div></body></html>");

        return html.toString();
    }

}
