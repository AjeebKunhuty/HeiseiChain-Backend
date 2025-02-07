package com.HeiseiChain.HeiseiChain.model;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Wallet {
    static {
        // Register the BouncyCastle provider
        Security.addProvider(new BouncyCastleProvider());
    }

    public PublicKey publicKey;
    public PrivateKey privateKey;
    public String role; // donor, volunteer, or camp

    public Wallet(String role) {
        this.role = role;
        generateKeyPair();

    }

    private void generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            keyGen.initialize(256, random);
            KeyPair keyPair = keyGen.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
            //System.out.println(role+"Public Key: " + publicKey);
            //System.out.println(role+"Private Key: " + privateKey);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<UTXO> getUTXOs(String metadata) {
        List<UTXO> utxos = new ArrayList<>();

        // Iterate through the global UTXO map and find UTXOs for this wallet
        for (TransactionOutput output : HeiseiChain.UTXOs.values()) {
            if (output.recipient.equals(this.publicKey) && output.commodity.equals(metadata)) {
                // Add UTXO to the list if it belongs to this wallet
                utxos.add(new UTXO(output.id, output.recipient, output.value,output.commodity));
            }
        }

        return utxos;
    }


}
