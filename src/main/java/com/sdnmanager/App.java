package com.sdnmanager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class App {
    private static final String RYU_BASE_URL = "http://127.0.0.1:8080";

    public static void main(String[] args) {
        System.out.println("=== Ryu SDN Controller API Client ===");

        try {
            fetchAndPrint("/v1.0/topology/switches", "Switches");
            fetchAndPrint("/v1.0/topology/links", "Links");
            fetchAndPrint("/v1.0/topology/hosts", "Hosts");
        } catch (Exception e) {
            System.out.println("Error connecting to Ryu Controller: " + e.getMessage());
        }
    }

    private static void fetchAndPrint(String endpoint, String label) throws IOException, InterruptedException {
        String url = RYU_BASE_URL + endpoint;
        System.out.println("\n--- " + label + " ---");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
    }
}

