package com.HeiseiChain.HeiseiChain.service;

import com.HeiseiChain.HeiseiChain.model.Block;
import com.HeiseiChain.HeiseiChain.model.Blockchain;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final Blockchain blockchain;

    // Inject the Blockchain instance via constructor
    public ReportService(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    // Fetch blocks based on date and time range
    private List<Block> fetchBlockchainData(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return blockchain.getChain().stream()
                .filter(block -> {
                    // Convert block timestamp (long) to LocalDateTime for comparison
                    LocalDateTime blockDateTime = Instant.ofEpochMilli(block.getTimestamp())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                    return !blockDateTime.isBefore(startDateTime) && !blockDateTime.isAfter(endDateTime);
                })
                .collect(Collectors.toList());
    }

    // Generate CSV report
    public ByteArrayInputStream generateReport(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<Block> blockchainData = fetchBlockchainData(startDateTime, endDateTime);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(out)) {

            // Write headers
            writer.println("Block Hash,Previous Hash,Timestamp,Transaction Count,Transaction Details");

            // Write details for each block
            for (Block block : blockchainData) {
                String transactionDetails = block.getTransactions().stream()
                        .map(tx -> "Sender: " + tx.getSender() +
                                ", Recipient: " + tx.getRecipient() +
                                ", Value: " + tx.getValue() +
                                ", Metadata: " + tx.getMetadata())
                        .collect(Collectors.joining(" | ")); // Separate transactions with a "|"

                writer.println(block.getHash() + "," +
                        block.getPreviousHash() + "," +
                        block.getFormattedTimestamp() + "," + // Includes date and time
                        block.getTransactions().size() + "," +
                        "\"" + transactionDetails + "\""); // Enclose transaction details in quotes
            }

            writer.flush();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Error generating report: " + e.getMessage());
        }
    }



}
