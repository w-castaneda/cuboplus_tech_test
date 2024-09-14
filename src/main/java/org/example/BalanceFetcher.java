package org.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

public class BalanceFetcher {

    private static final String ADDRESS = "32ixEdVJWo3kmvJGMTZq5jAQVZZeuwnqzo";
    private static final String API_URL = "https://mempool.space/api/address/" + ADDRESS;

    public static void main(String[] args) {
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
                    long confirmedBalance = fundedTxoSumChain - spentTxoSumChain;
                    System.out.println("On-chain balance (in satoshis): " + confirmedBalance);
                    System.out.println("On-chain balance (in BTC): " + (confirmedBalance / 100_000_000.0));
                } else {
                    System.out.println("The following fields were not found 'funded_txo_sum' or 'spent_txo_sum' in 'chain_stats'.");
                }
            } else {
                System.out.println("Object not Found 'chain_stats' in the response.");
            }

            System.out.println("-------------------------------------------------");

            // Getting the mempool statistics.
            JSONObject mempoolStats = jsonResponse.optJSONObject("mempool_stats");
            if (mempoolStats != null) {
                long fundedTxoSumMempool = mempoolStats.optLong("funded_txo_sum", -1);
                long spentTxoSumMempool = mempoolStats.optLong("spent_txo_sum", -1);

                if (fundedTxoSumMempool != -1 && spentTxoSumMempool != -1) {
                    long mempoolBalance = fundedTxoSumMempool - spentTxoSumMempool;
                    System.out.println("Mempool Balance (in satoshis): " + mempoolBalance);
                    System.out.println("Mempool Balance (in BTC): " + (mempoolBalance / 100_000_000.0));
                } else {
                    System.out.println("The following fields were not found 'funded_txo_sum' or 'spent_txo_sum' in 'mempool_stats'.");
                }
            } else {
                System.out.println("Object not Found 'mempool_stats' in the response.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
