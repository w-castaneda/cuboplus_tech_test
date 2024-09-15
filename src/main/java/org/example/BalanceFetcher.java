package org.example;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

public class BalanceFetcher {

    private static final String ADDRESS = "32ixEdVJWo3kmvJGMTZq5jAQVZZeuwnqzo";
    private static final String API_URL = "https://mempool.space/api/address/" + ADDRESS;
    private static final String BALANCE_FILE_PATH = "balance_last_7_days.txt";

    public static void main(String[] args) {
        getMempoolBalance();
        getVariationBalance();
    }

    public static long getOnChainBalance(){
        long confirmedBalance = 0;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject jsonResponse = new JSONObject(response.body());

            // getting the chain statistics
            JSONObject chainStats = jsonResponse.optJSONObject("chain_stats");
            if (chainStats != null) {
                long fundedTxoSumChain = chainStats.optLong("funded_txo_sum", -1);
                long spentTxoSumChain = chainStats.optLong("spent_txo_sum", -1);

                if (fundedTxoSumChain != -1 && spentTxoSumChain != -1) {
                    confirmedBalance = fundedTxoSumChain - spentTxoSumChain;
                    System.out.println("On-chain balance (in satoshis): " + confirmedBalance);
                    //System.out.println("On-chain balance (in BTC): " + (confirmedBalance / 100_000_000.0));
                    System.out.println("----------------------------------------------------------------");
                } else {
                    System.out.println("The following fields were not found 'funded_txo_sum' or 'spent_txo_sum' in 'chain_stats'.");
                }
            } else {
                System.out.println("Object not Found 'chain_stats' in the response.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return confirmedBalance;
    }

    public static void getMempoolBalance() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject jsonResponse = new JSONObject(response.body());
            // Getting the mempool statistics.
            JSONObject mempoolStats = jsonResponse.optJSONObject("mempool_stats");
            if (mempoolStats != null) {
                long fundedTxoSumMempool = mempoolStats.optLong("funded_txo_sum", -1);
                long spentTxoSumMempool = mempoolStats.optLong("spent_txo_sum", -1);

                if (fundedTxoSumMempool != -1 && spentTxoSumMempool != -1) {
                    long mempoolBalance = fundedTxoSumMempool - spentTxoSumMempool;
                    System.out.println("----------------------------------------------------------------");
                    System.out.println("Mempool Balance (in satoshis): " + mempoolBalance);
                    //System.out.println("Mempool Balance (in BTC): " + (mempoolBalance / 100_000_000.0));
                    System.out.println("----------------------------------------------------------------");
                } else {
                    System.out.println("The following fields were not found 'funded_txo_sum' or 'spent_txo_sum' in 'mempool_stats'.");
                }
            } else {
                System.out.println("Object not Found 'mempool_stats' in the response.");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void getVariationBalance() {
        // Create a ScheduledExecutorService
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Schedule the task to get the current balance and calculate variation
        scheduler.schedule(() -> {
            try {
                // Get the current balance
                long currentBalance = getOnChainBalance();
                System.out.println("Confirmed Current Balance: " + currentBalance + " satoshis");

                // Get the balance from 7 days ago
                long pastBalance = readPastBalance();
                if (pastBalance == -1) {
                    System.out.println("Balance from the last 7 days Not Found.");
                } else {
                    System.out.println("Confirmed Balance from the last 7 days: " + pastBalance + " satoshis");
                    System.out.println("----------------------------------------------------------------");
                    // Calculate the balance variation
                    long balanceVariation = currentBalance - pastBalance;
                    System.out.println("Confirmed Variation Balance from the last 7 days: " + balanceVariation + " satoshis");
                    //System.out.println("Confirmed Balance Variation in the last 7 days: " + (balanceVariation / 100_000_000.0) + " BTC");
                }

                // Save the current balance for future comparison
                saveCurrentBalance(currentBalance);

            } catch (Exception e) {
                e.printStackTrace();
            }
            // Shut down the scheduler after the task is complete
            scheduler.shutdown();
        }, 0, TimeUnit.SECONDS); // Change to the appropriate delay if needed
    }

    private static void saveCurrentBalance(long balance) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(BALANCE_FILE_PATH))) {
            writer.write(Long.toString(balance));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static long readPastBalance() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(BALANCE_FILE_PATH)));
            return Long.parseLong(content);
        } catch (IOException | NumberFormatException e) {
            return -1;
        }
    }

}
