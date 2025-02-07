package com.HeiseiChain.HeiseiChain.model;

public class TransactionInput {
    public String transactionOutputId; // Reference to the TransactionOutput being spent
    public TransactionOutput UTXO; // The Unspent Transaction Output being used in this input

    // Constructor for creating a new TransactionInput
    public TransactionInput(String transactionOutputId) {
        this.transactionOutputId = transactionOutputId;
    }
}
