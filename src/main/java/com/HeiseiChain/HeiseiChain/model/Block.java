package com.HeiseiChain.HeiseiChain.model;

import java.util.List;

public class Block {
    private String hash;
    private String previousHash;
    private List<Transaction> transactions;
    private long timestamp;

    public Block(List<Transaction> transactions, String previousHash) {
        this.transactions = transactions;
        this.previousHash = previousHash;
        this.timestamp = System.currentTimeMillis()+ (5 * 60 + 30) * 60 * 1000;
        this.hash = calculateHash();
    }

    public String calculateHash() {
        StringBuilder input = new StringBuilder();
        input.append(previousHash);
        input.append(Long.toString(timestamp));

        for (Transaction tx : transactions) {
            input.append(tx.getTransactionId());
        }

        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(input.toString());
    }

    public String getFormattedTimestamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
    }

    // Getters and Setters
    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
