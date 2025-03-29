package com.HeiseiChain.HeiseiChain.model;

import org.springframework.stereotype.Component;

import java.security.PublicKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component//to make it bean for injection at blockchain service
public class Blockchain {
    private final List<Block> chain;
    private List<Transaction> currentTransactions;

    public Blockchain() {
        chain = new ArrayList<>();
        currentTransactions = new ArrayList<>();
        chain.add(createGenesisBlock());
    }

    private Block createGenesisBlock() {
        return new Block(new ArrayList<>(), "0"); // Genesis block has no previous hash
    }

    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    public void addTransaction(Transaction transaction) {
        currentTransactions.add(transaction);
        if (!currentTransactions.isEmpty()) {  // Add block when the list reaches a predefined size
            addBlock(new Block(currentTransactions, getLatestBlock().getHash()));
            currentTransactions = new ArrayList<>();  // Reset transactions list after adding block
        }
        HeiseiChain.processUTXOs(transaction);
    }

    public void addBlock(Block newBlock) {
        newBlock.setPreviousHash(getLatestBlock().getHash());
        newBlock.setHash(newBlock.calculateHash());
        chain.add(newBlock);
    }

    public boolean isChainValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block currentBlock = chain.get(i);
            Block previousBlock = chain.get(i - 1);

            // Validate hash and previous hash
            if (!currentBlock.getHash().equals(currentBlock.calculateHash())) {
                return false;
            }

            if (!currentBlock.getPreviousHash().equals(previousBlock.getHash())) {
                return false;
            }
        }
        return true;
    }

    public List<Block> getChain() {
        return chain;
    }

    public String displayHTML(Map<String, Wallet> walletDatabase) {
        StringBuilder data = new StringBuilder();
        data.append("<div class=\"container\">");

        for (int i = 0; i < chain.size(); i++) {
            Block block = chain.get(i);
            data.append("<div class=\"block\">")
                    .append("<div class=\"block-header\">Block ").append(i).append("</div>")
                    .append("<div class=\"block-body\">")
                    .append("<p><strong>Hash:</strong> ").append(block.getHash()).append("</p>")
                    .append("<p><strong>Previous Hash:</strong> ").append(block.getPreviousHash()).append("</p>")
                    .append("<p><strong>Timestamp:</strong> ").append(block.getFormattedTimestamp()).append("</p>");

            if (block.getTransactions() != null && !block.getTransactions().isEmpty()) {
                data.append("<div class=\"transactions\">");
                for (Transaction transaction : block.getTransactions()) {
                    data.append("<div class=\"transaction\">")
                            .append("<p><strong>Sender:</strong> ").append(findUserByPublicKey(transaction.getSender(), walletDatabase)).append("</p>")
                            .append("<p><strong>Recipient:</strong> ").append(findUserByPublicKey(transaction.getRecipient(), walletDatabase)).append("</p>")
                            .append("<p><strong>Value:</strong> ").append(transaction.getValue()).append("</p>")
                            .append("<p><strong>Metadata:</strong> ").append(transaction.getMetadata()).append("</p>")
                            .append("</div>");
                }
                data.append("</div>");
            } else {
                data.append("<div class=\"transactions\">")
                        .append("<p class=\"no-transactions\">No transactions in this block.</p>")
                        .append("</div>");
            }
            data.append("</div></div>");
        }

        data.append("</div>");
        return data.toString();
    }

    public String findUserByPublicKey(PublicKey key, Map<String, Wallet> walletDatabase) {
        for (Map.Entry<String, Wallet> entry : walletDatabase.entrySet()) {
            if (entry.getValue().publicKey.equals(key)) {
                return entry.getKey();
            }
        }
        return "Unknown User";
    }

    public String generateCSVReport(Map<String, Wallet> walletDatabase, Map<String, Object[]> pendingTransactions, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        StringBuilder csvData = new StringBuilder();

        // Adding CSV headers
        csvData.append("Block Number,Hash,Previous Hash,Timestamp,Sender,Recipient,Commodity,Value\n");

        for (int i = 0; i < chain.size(); i++) {
            Block block = chain.get(i);

            if(block.getPreviousHash().equals("0"))
                continue;

            // Convert block timestamp to LocalDateTime
            LocalDateTime blockDateTime = Instant.ofEpochMilli(block.getTimestamp())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            // Filter blocks by date range
            if (blockDateTime.isBefore(startDateTime) || blockDateTime.isAfter(endDateTime)) {
                continue;
            }

            // Prepare transaction details
            StringBuilder transactionDetails = new StringBuilder();
            if (block.getTransactions() != null && !block.getTransactions().isEmpty()) {
                for (Transaction transaction : block.getTransactions()) {
                    if(transaction.getMetadata().contains("User Registration of"))
                        continue;
                    transactionDetails.append("Sender: ")
                            .append(findUserByPublicKey(transaction.getSender(), walletDatabase))
                            .append(" | Recipient: ")
                            .append(findUserByPublicKey(transaction.getRecipient(), walletDatabase))
                            .append(" | Value: ")
                            .append(transaction.getValue())
                            .append(" | Metadata: ")
                            .append(transaction.getMetadata())
                            .append(" || "); // Separator for transactions

                    csvData.append(i).append(",")
                            .append(block.getHash()).append(",")
                            .append(block.getPreviousHash()).append(",")
                            .append(block.getFormattedTimestamp()).append(",")
                            .append(findUserByPublicKey(transaction.getSender(), walletDatabase)).append(",")
                            .append(findUserByPublicKey(transaction.getRecipient(), walletDatabase)).append(",")
                            .append(transaction.getMetadata()).append(",")
                            .append(transaction.getValue()).append(",")
                            .append("\n");

                }
            } else {
                csvData.append(i).append(",")
                        .append(block.getHash()).append(",")
                        .append(block.getPreviousHash()).append(",")
                        .append(block.getFormattedTimestamp()).append(",")
                        .append(block.getTransactions() != null ? block.getTransactions().size() : 0).append(",")
                        .append("\"").append("No transactions in this block").append("\"\n"); // Enclose transactions in quotes to handle commas

//                transactionDetails.append();
            }

            // Add block data to CSV
        }

        // **Adding a separator for Pending Transactions**
        csvData.append("\nUnconfirmed Transactions Table:\n");
        csvData.append("Number,Sender,Recipient,Commodity,Quantity,Time\n");

        int count = 0;
        // **Appending pending transactions**
        for (Map.Entry<String, Object[]> entry : pendingTransactions.entrySet()) {
            Object[] pendingTx = entry.getValue();
            csvData.append(++count).append(",")  // Transaction Number (Key)
                    .append(findUserByPublicKey((PublicKey) pendingTx[0], walletDatabase)).append(",")
                    .append(findUserByPublicKey((PublicKey) pendingTx[1], walletDatabase)).append(",")
                    .append(pendingTx[3]).append(",")
                    .append(pendingTx[2]).append(",")
                    .append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date((Long) pendingTx[6])))
                    .append("\n");
        }

        return csvData.toString();
    }

}