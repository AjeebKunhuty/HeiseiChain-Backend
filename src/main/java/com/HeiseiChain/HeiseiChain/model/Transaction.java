package com.HeiseiChain.HeiseiChain.model;

import com.HeiseiChain.HeiseiChain.util.StringUtil;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Transaction {

    public String transactionId; // This is also the hash of the transaction.
    public PublicKey sender; // Sender's address/public key.
    public PublicKey recipient; // Recipient's address/public key.
    public String transactionType; // Donation or Volunteer
    public float value; // Value of donation (money or goods)
    public String metadata; // Additional information like goods type, quantity, etc.
    public byte[] signature; // Signature to ensure authenticity
    public long creationTime;

    public ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>(); // List of transaction inputs
    public ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>(); // List of transaction outputs

    private static int sequence = 0; // A rough count of how many transactions have been generated.

    // Constructor: Initializes a new transaction with the given parameters.
    public Transaction(PublicKey from, PublicKey to, String transactionType, float value, String metadata, ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.recipient = to;
        this.transactionType = transactionType;
        this.value = value;
        this.metadata = metadata;
        this.inputs = inputs;
        this.outputs = new ArrayList<>();
        this.transactionId = calculateTransactionId();  // Ensure ID is calculated correctly here
        this.creationTime = System.currentTimeMillis()+ (5 * 60 + 30) * 60 * 1000;
    }

    // Calculates the unique transaction ID based on transaction details (sender, recipient, value, metadata, inputs, outputs)
    private String calculateTransactionId() {
        sequence++;
        StringBuilder inputsData = new StringBuilder();
        for (TransactionInput input : inputs) {
            inputsData.append(input.transactionOutputId);  // Include the input's transaction output ID
        }

        StringBuilder outputsData = new StringBuilder();
        for (TransactionOutput output : outputs) {
            outputsData.append(output.id).append(output.recipient).append(output.value);
        }

        // Generate a unique ID by hashing the concatenation of all relevant data.
        return StringUtil.applySha256(
                StringUtil.getStringFromKey(sender) +
                        StringUtil.getStringFromKey(recipient) +
                        Float.toString(value) +
                        metadata +
                        inputsData.toString() +
                        outputsData.toString()+
                        sequence
        );
    }

    // Generates a hash of the transaction (used for transaction ID)
    private String calculateHash() {
        sequence++; // Increase the sequence to avoid identical transactions having the same hash
        return StringUtil.applySha256(
                StringUtil.getStringFromKey(sender) +
                        StringUtil.getStringFromKey(recipient) +
                        transactionType +
                        Float.toString(value) +
                        metadata +
                        sequence
        );
    }

    // Signs the transaction data with the sender's private key
    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) +
                StringUtil.getStringFromKey(recipient) +
                transactionType +
                Float.toString(value) +
                metadata;
        //System.out.println("Data for signing: " + data);
        signature = StringUtil.applyECDSASig(privateKey, data); // Apply the ECDSA signature
    }

    // Verifies the transaction signature using the sender's public key
    public boolean verifySignature() {
        String data = StringUtil.getStringFromKey(sender) +
                StringUtil.getStringFromKey(recipient) +
                transactionType +
                Float.toString(value) +
                metadata;
        //System.out.println("Data for verification: " + data);
        return StringUtil.verifyECDSASig(sender, data, signature); // Verify the signature
    }

    // Processes the transaction (checks validity, applies changes to the blockchain)
    public Set<PublicKey> processTransaction() {
        if (!verifySignature()) {
            System.out.println("# Transaction Signature failed to verify");
            return null;
        }

        // Process inputs (check if unspent)
        for (TransactionInput i : inputs) {
            i.UTXO = HeiseiChain.UTXOs.get(i.transactionOutputId);  // Get the UTXO for each input
        }

        // Check if transaction is valid (e.g., enough balance)
        if (getInputsValue() < HeiseiChain.minimumTransaction) {
            System.out.println("# Transaction Inputs too small: " + getInputsValue());
            return null;
        }

        // Track all unique donors from inputs
        Set<PublicKey> donors = new HashSet<>();
        for (TransactionInput i : inputs) {
            if (i.UTXO != null && i.UTXO.donor != null) {
                donors.addAll(i.UTXO.donor);
            }
        }

        // If no previous donors exist (i.e., direct donation), set sender as the only donor
        if (donors.isEmpty()) {
            donors.add(this.sender);
        }

        // Generate outputs (donation or volunteer service)
        float leftover = getInputsValue() - value; // Calculate leftover change after transaction
        outputs.add(new TransactionOutput(this.recipient, value, transactionId,metadata,donors)); // Send value to recipient
        if (leftover > 0) {
            outputs.add(new TransactionOutput(this.sender, leftover, transactionId,metadata,donors)); // Return leftover change to sender
        }

        // Add outputs to UTXO list
        for (TransactionOutput o : outputs) {
            HeiseiChain.UTXOs.put(o.id, o);  // Update UTXOs map
        }

        // Remove spent inputs from UTXO list
        for (TransactionInput i : inputs) {
            if (i.UTXO == null) continue; // Skip if UTXO is not found
            HeiseiChain.UTXOs.remove(i.UTXO.id);
        }

        return donors;
    }

    // Returns the sum of input values
    public float getInputsValue() {
        if (inputs == null || inputs.isEmpty()) {
            // Allow transactions without inputs (e.g., donations)
            return value; // Use transaction value for validation
        }

        float total = 0;
        for (TransactionInput input : inputs) {
            if (input.UTXO == null) continue; // Skip if input is not valid
            total += input.UTXO.value;  // Sum up the input values
        }
        return total;
    }

    // Returns the sum of output values
    public float getOutputsValue() {
        float total = 0;
        for (TransactionOutput o : outputs) {
            total += o.value;  // Sum up the output values
        }
        return total;
    }

    // Add outputs to UTXOs after transaction
    public void addOutputsToUTXOs() {
        for (TransactionOutput output : this.outputs) {
            HeiseiChain.UTXOs.put(output.id, output);  // Update UTXOs map
        }
    }

    // Getters and Setters
    public String getTransactionId() {
        return transactionId;
    }

    public PublicKey getSender() {
        return sender;
    }

    public PublicKey getRecipient() {
        return recipient;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public float getValue() {
        return value;
    }

    public String getMetadata() {
        return metadata;
    }

    public byte[] getSignature() {
        return signature;
    }

    public ArrayList<TransactionInput> getInputs() {
        return inputs;
    }

    public ArrayList<TransactionOutput> getOutputs() {
        return outputs;
    }

    public String getType() {
        return transactionType;
    }
}
