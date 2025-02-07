package com.HeiseiChain.HeiseiChain.model;

import java.security.PublicKey;
import java.util.*;

public class HeiseiChain {
    public static Map<String, TransactionOutput> UTXOs = new HashMap<>();  // Unspent Transaction Outputs
    public static float minimumTransaction = 0.1f;

    // This method adds an output to the UTXO set
    public static void addUTXO(TransactionOutput output) {
        UTXOs.put(output.id, output);
    }

    // This method processes the UTXO set by removing used outputs and adding new ones
    public static void processUTXOs(Transaction transaction) {
        // Add new outputs to the UTXO set
        for (TransactionOutput output : transaction.outputs) {
            addUTXO(output);
        }

        // Remove used UTXOs (spent outputs)
        for (TransactionInput input : transaction.inputs) {
            if (input.UTXO != null) {
                UTXOs.remove(input.UTXO.id);
            }
        }
    }

    // This method finds an unspent output (UTXO) by its ID
    public static TransactionOutput getUTXO(String outputId) {
        return UTXOs.get(outputId);
    }

    // This method prints the current UTXO set for debugging purposes
    public static void printUTXOs() {
        for (Map.Entry<String, TransactionOutput> entry : UTXOs.entrySet()) {
            System.out.println("UTXO ID: " + entry.getKey() + ", Value: " + entry.getValue().value);
        }
    }

    // This method returns a list of all transactions (for demo purposes)
    public static List<Transaction> getAllTransactions() {
        // Example: Return a static list of transactions (can be fetched from blockchain data)
        // In a real scenario, this would return transactions from the actual blockchain
        return new ArrayList<>();
    }
}
