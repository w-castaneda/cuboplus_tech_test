package org.example;

import java.io.*;
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

public class BalanceVariationFetcher {

    private static final String ADDRESS = "32ixEdVJWo3kmvJGMTZq5jAQVZZeuwnqzo";
    private static final String API_URL = "https://mempool.space/api/address/" + ADDRESS;
    private static final String BALANCE_FILE_PATH = "balance_last_7_days.txt";

    public static void main(String[] args) {
        // Create a ScheduledExecutorService
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Schedule the task to get the current balance and calculate variation
        scheduler.schedule(() -> {
            try {
                // Get the current balance
                long currentBalance = getConfirmedBalance();
                System.out.println("Balance confirmado actual: " + currentBalance + " satoshis");

                // Get the balance from 7 days ago
                long pastBalance = readPastBalance();
                if (pastBalance == -1) {
                    System.out.println("No se encontró el balance de hace 7 días.");
                } else {
                    System.out.println("Balance confirmado hace 7 días: " + pastBalance + " satoshis");

                    // Calculate the balance variation
                    long balanceVariation = currentBalance - pastBalance;
                    System.out.println("Variación del balance confirmado en los últimos 7 días: " + balanceVariation + " satoshis");
                    System.out.println("Variación del balance confirmado en los últimos 7 días: " + (balanceVariation / 100_000_000.0) + " BTC");
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

    private static long getConfirmedBalance() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject jsonResponse = new JSONObject(response.body());

        // Obtener estadísticas de la cadena
        JSONObject chainStats = jsonResponse.optJSONObject("chain_stats");
        if (chainStats != null) {
            long fundedTxoSum = chainStats.optLong("funded_txo_sum", -1);
            long spentTxoSum = chainStats.optLong("spent_txo_sum", -1);

            if (fundedTxoSum != -1 && spentTxoSum != -1) {
                return fundedTxoSum - spentTxoSum;
            } else {
                throw new Exception("No se encontraron los campos 'funded_txo_sum' o 'spent_txo_sum' en 'chain_stats'.");
            }
        } else {
            throw new Exception("No se encontró el objeto 'chain_stats' en la respuesta.");
        }
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

