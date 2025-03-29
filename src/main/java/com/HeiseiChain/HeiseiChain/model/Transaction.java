package com.HeiseiChain.HeiseiChain.model;

import com.HeiseiChain.HeiseiChain.util.StringUtil;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;

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
    public Transaction(PublicKey from, PublicKey to, String transactionType, float value, String metadata, ArrayList<TransactionInput> inputs, long creationTime) {
        this.sender = from;
        this.recipient = to;
        this.transactionType = transactionType;
        this.value = value;
        this.metadata = metadata;
        this.inputs = inputs;
        this.outputs = new ArrayList<>();
        this.transactionId = calculateTransactionId();  // Ensure ID is calculated correctly here
        this.creationTime = creationTime;
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
    public synchronized Map<PublicKey, Float> processTransaction() {
        if (!verifySignature()) {
            System.out.println("# Transaction Signature failed to verify");
            return null;
        }

        // 🔹 Step 1: Check if UTXOs are already being used in another transaction
        for (TransactionInput i : inputs) {
            if (HeiseiChain.pendingUTXOs.contains(i.transactionOutputId)) {
                System.out.println("# UTXO is pending in another transaction, retrying...");
                waitForUTXOs();
            }
        }

        // 🔹 Step 2: Lock UTXOs for this transaction
        for (TransactionInput i : inputs) {
            HeiseiChain.pendingUTXOs.add(i.transactionOutputId);
        }

        // 🔹 Step 3: Retrieve UTXOs and validate the transaction
        for (TransactionInput i : inputs) {
            i.UTXO = HeiseiChain.UTXOs.get(i.transactionOutputId);
        }

        if (getInputsValue() < HeiseiChain.minimumTransaction) {
            System.out.println("# Transaction Inputs too small: " + getInputsValue());
            releaseUTXOs();
            return null;
        }

        // 🔹 Step 4: Process the transaction (same as before)
        Map<PublicKey, Float> donors = new HashMap<>();
        for (TransactionInput i : inputs) {
            if (i.UTXO != null && i.UTXO.donor != null) {
                for (Map.Entry<PublicKey, Float> entry : i.UTXO.donor.entrySet()) {
                    donors.put(entry.getKey(), donors.getOrDefault(entry.getKey(), 0.0f) + entry.getValue());
                }
            }
        }

        if (donors.isEmpty()) {
            donors.put(this.sender, value);
        }

        float remaining = value;
        Map<PublicKey, Float> usedContributions = new LinkedHashMap<>();
        for (Iterator<Map.Entry<PublicKey, Float>> it = donors.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<PublicKey, Float> entry = it.next();
            PublicKey donor = entry.getKey();
            float available = entry.getValue();

            if (remaining > 0) {
                float used = Math.min(available, remaining);
                usedContributions.put(donor, used);
                remaining -= used;
            } else {
                break;
            }
        }

        float available = getInputsValue();
        float leftover = available - value;
        outputs.add(new TransactionOutput(this.recipient, value, transactionId, metadata, usedContributions));
        if (leftover > 0) {
            outputs.add(new TransactionOutput(this.sender, leftover, transactionId, metadata, donors));
        }

        for (TransactionOutput o : outputs) {
            HeiseiChain.UTXOs.put(o.id, o);
        }

        for (TransactionInput i : inputs) {
            if (i.UTXO == null) continue;
            HeiseiChain.UTXOs.remove(i.UTXO.id);
        }

        // 🔹 Step 5: Release UTXOs after successful transaction
        releaseUTXOs();

        return usedContributions;
    }

    // 🔹 Helper method: Wait and retry if UTXOs are locked
    private void waitForUTXOs() {
        try {
            Thread.sleep(100); // Small delay before retrying
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 🔹 Helper method: Release UTXOs after processing
    private void releaseUTXOs() {
        for (TransactionInput i : inputs) {
            HeiseiChain.pendingUTXOs.remove(i.transactionOutputId);
        }
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
