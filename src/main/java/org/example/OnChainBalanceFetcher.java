package org.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

public class OnChainBalanceFetcher {

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

            // Obtener las estadísticas de la cadena
            JSONObject chainStats = jsonResponse.optJSONObject("chain_stats");
            if (chainStats != null) {
                long fundedTxoSum = chainStats.optLong("funded_txo_sum", -1);
                long spentTxoSum = chainStats.optLong("spent_txo_sum", -1);

                if (fundedTxoSum != -1 && spentTxoSum != -1) {
                    long balanceConfirmed = fundedTxoSum - spentTxoSum;
                    System.out.println("Balance confirmado en la cadena de bloques (en satoshis): " + balanceConfirmed);
                    System.out.println("Balance confirmado en la cadena de bloques (en BTC): " + (balanceConfirmed / 100_000_000.0));
                } else {
                    System.out.println("No se encontraron los campos 'funded_txo_sum' o 'spent_txo_sum'.");
                }
            } else {
                System.out.println("No se encontró el objeto 'chain_stats' en la respuesta.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
