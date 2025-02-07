package com.HeiseiChain.HeiseiChain.service;

import com.HeiseiChain.HeiseiChain.model.*;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class BlockchainService {

    private final Blockchain blockchain;
    private Map<String, Wallet> walletDatabase = new HashMap<>();

    public BlockchainService() {
        this.blockchain = new Blockchain();
    }

    public String getBlockchain() {
        return blockchain.displayHTML(walletDatabase);
    }

    public String getReport(LocalDateTime startDateTime, LocalDateTime endDateTime) { return blockchain.generateCSVReport(walletDatabase,startDateTime,endDateTime);}

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





}
