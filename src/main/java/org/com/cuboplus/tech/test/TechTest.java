package org.com.cuboplus.tech.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TechTest {

    //Address of El Salvador Bitcoin treasury holdings
    private static final String ADDRESS = "32ixEdVJWo3kmvJGMTZq5jAQVZZeuwnqzo";
    //API_URL_MEMPOOL - to calculate mempool balance based on confirmed transactions
    private static final String API_URL_MEMPOOL = "https://mempool.space/api/address/" + ADDRESS + "/txs/mempool";
    //API_URL_TXS - to calculate the On-Chain balance based also on confirmed transactions
    private static final String API_URL_TXS = "https://mempool.space/api/address/" + ADDRESS + "/txs";
    //FILE_PATH - File where the On-chain balance is stored.
    private static final String FILE_PATH = "balance_log.txt";

    public static void main(String[] args) {
        try {
            //We can modify this value to obtain the 7 days balance variation or 30 days as requested in the Technical test
            int daysAgo = 7;
            // Parameter of 30 days
            //int daysAgo = 30;

            // Getting the current balance
            long onChainBalance = getOnChainBalance(ADDRESS);

            // Getting the Mempool Balance
            long MemPoolTotalBalance = getMempoolBalance(ADDRESS);

            // Getting the date for the period requested for 7 or 30 days
            String dateDaysAgo = getDateDaysAgo(daysAgo);

            // Saving the current balance to a file
            String currentDate = getCurrentDate();
            saveBalanceToFile(onChainBalance, currentDate);

            // Getting the balance of the number of days previously specified 7 or 30
            long pastBalance = getBalanceForDate(dateDaysAgo);

            // Show the current balance and the variation
            if (pastBalance != -1) {
                long variation = onChainBalance - pastBalance;
                System.out.println("---------------------------------------------------------------------------");
                System.out.println("On-Chain balance: " + onChainBalance + " satoshis");
                System.out.println("On-Chain balance: " + convertSatoshisToBTC(onChainBalance) + " bitcoins");
                System.out.println("---------------------------------------------------------------------------");
                System.out.println("MemPool balance: " + MemPoolTotalBalance + " satoshis");
                System.out.println("MemPool balance: " + convertSatoshisToBTC(MemPoolTotalBalance) + " bitcoins");
                System.out.println("---------------------------------------------------------------------------");
                System.out.println("On-Chain Balance " + daysAgo + " days ago: " + pastBalance + " satoshis");
                System.out.println("On-Chain Balance " + daysAgo + " days ago: " + convertSatoshisToBTC(pastBalance) + " bitcoins");
                System.out.println("---------------------------------------------------------------------------");
                System.out.println("Balance variation in a Period of " + daysAgo + " days ago: " + daysAgo + variation + " satoshis");
                System.out.println("Balance variation in a Period of " + daysAgo + " days ago: " + convertSatoshisToBTC(variation) + " bitcoins");
            } else {
                System.out.println("No balance found for the date " + daysAgo + " days ago.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Method to get the current date to be used when saving the on-Chain balance
    private static String getCurrentDate() {
        Instant instant = Instant.now();
        ZonedDateTime dateTime = instant.atZone(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }

    //Method to format the date that it is going to be used to search it on the file
    private static String getDateDaysAgo(int daysAgo) {
        Instant instant = Instant.now().minusSeconds(daysAgo * 86400L); // 86400 seconds in a day
        ZonedDateTime dateTime = instant.atZone(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Getting the date
        return dateTime.format(formatter);
    }

    //Method to save the balance to the balance_log.txt
    private static void saveBalanceToFile(long balance, String date) throws IOException {
        String logEntry = date + " - Balance: " + balance + " satoshis\n";
        Files.write(Paths.get(FILE_PATH), logEntry.getBytes(),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
    }

    //Method to find the balance according to the specific date
    private static long getBalanceForDate(String dateToFind) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(FILE_PATH));
        String patternString = "^" + Pattern.quote(dateToFind) + ".* - Balance: (\\d+) satoshis";
        Pattern pattern = Pattern.compile(patternString);

        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        }
        return -1;
    }

    //Method to get and calculate the On-Chain balance
    private static long getOnChainBalance(String address) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL_TXS))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JSONArray txArray = new JSONArray(response.body());
        long totalOnChainBalance = 0;

        for (int i = 0; i < txArray.length(); i++) {
            JSONObject tx = txArray.getJSONObject(i);
            JSONArray outputs = tx.getJSONArray("vout");
            JSONArray inputs = tx.getJSONArray("vin");

            // Subtract values from inputs
            for (int j = 0; j < inputs.length(); j++) {
                JSONObject input = inputs.getJSONObject(j);
                if (input.has("prevout")) {
                    JSONObject prevout = input.getJSONObject("prevout");
                    if (prevout.has("scriptpubkey_address") && prevout.getString("scriptpubkey_address").equals(address)) {
                        long value = prevout.getLong("value");
                        totalOnChainBalance -= value;
                    }
                }
            }

            // Add values from outputs
            for (int k = 0; k < outputs.length(); k++) {
                JSONObject output = outputs.getJSONObject(k);
                if (output.has("scriptpubkey_address") && output.getString("scriptpubkey_address").equals(address)) {
                    long value = output.getLong("value");
                    totalOnChainBalance += value;
                }
            }
        }

        return totalOnChainBalance;
    }

    //Method to get the mempool balance according to confirmed transactions
    private static long getMempoolBalance(String address) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL_MEMPOOL))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Printing the full response for testing and verification purposes.
        //System.out.println("Mempool Response: " + response.body());

        JSONArray txArray = new JSONArray(response.body());
        long totalMemPoolBalance = 0;

        for (int i = 0; i < txArray.length(); i++) {
            JSONObject tx = txArray.getJSONObject(i);
            JSONArray inputs = tx.getJSONArray("vin");
            JSONArray outputs = tx.getJSONArray("vout");

            // Verifying inputs
            for (int j = 0; j < inputs.length(); j++) {
                JSONObject input = inputs.getJSONObject(j);
                if (input.has("prevout")) {
                    JSONObject prevout = input.getJSONObject("prevout");
                    if (prevout.has("scriptpubkey_address") && prevout.getString("scriptpubkey_address").equals(address)) {
                        // Restar el valor de la entrada del balance
                        long value = prevout.getLong("value");
                        totalMemPoolBalance -= value;
                    }
                }
            }

            // Verifying outputs
            for (int k = 0; k < outputs.length(); k++) {
                JSONObject output = outputs.getJSONObject(k);
                if (output.has("scriptpubkey_address") && output.getString("scriptpubkey_address").equals(address)) {
                    // Sumar el valor de la salida al balance
                    long value = output.getLong("value");
                    totalMemPoolBalance += value;
                }
            }
        }

        return totalMemPoolBalance;
    }

    //Method to convert previousl saved Satoshis to bitcoins
    private static double convertSatoshisToBTC(long satoshis) {
        return satoshis / 100_000_000.0;
    }
}
