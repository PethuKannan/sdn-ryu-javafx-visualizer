package com.sdnmanager;

import java.io.*;
import java.net.*;
import java.util.*;

public class RyuClient {

    private static final String CONTROLLER_URL = "http://127.0.0.1:8080";

    public static void main(String[] args) throws IOException {
        System.out.println("=== Ryu SDN Controller API Client ===");
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n--- MENU ---");
            System.out.println("1. Show Switches");
            System.out.println("2. Show Links");
            System.out.println("3. Show Hosts");
            System.out.println("4. Add Flow Rule");
            System.out.println("5. Delete Flow Rule");
            System.out.println("6. Exit");
            System.out.print("Enter choice: ");
            int choice = sc.nextInt();

            switch (choice) {
                case 1:
                    sendGetRequest("/v1.0/topology/switches");
                    break;
                case 2:
                    sendGetRequest("/v1.0/topology/links");
                    break;
                case 3:
                    sendGetRequest("/v1.0/topology/hosts");
                    break;
                case 4:
                    addFlowRule(sc);
                    break;
                case 5:
                    deleteFlowRule(sc);
                    break;
                case 6:
                    System.out.println("Exiting...");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid choice. Try again!");
            }
        }
    }

    private static void sendGetRequest(String endpoint) {
        try {
            URL url = new URL(CONTROLLER_URL + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();
            System.out.println(response.toString());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void addFlowRule(Scanner sc) {
        try {
            sc.nextLine(); // clear buffer
            System.out.print("Enter switch DPID (e.g., 0000000000000001): ");
            String dpid = sc.nextLine();

            System.out.print("Enter input port number: ");
            int inPort = sc.nextInt();

            System.out.print("Enter output port number: ");
            int outPort = sc.nextInt();

            String json = "{"
                    + "\"dpid\": " + Long.parseLong(dpid) + ","
                    + "\"cookie\": 1,"
                    + "\"priority\": 100,"
                    + "\"match\": {\"in_port\": " + inPort + "},"
                    + "\"actions\": [{\"type\": \"OUTPUT\", \"port\": " + outPort + "}]"
                    + "}";

            URL url = new URL(CONTROLLER_URL + "/stats/flowentry/add");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes());
            os.flush();
            os.close();

            System.out.println("Response Code: " + conn.getResponseCode());
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();

        } catch (Exception e) {
            System.out.println("Error adding flow rule: " + e.getMessage());
        }
    }

    private static void deleteFlowRule(Scanner sc) {
        try {
            sc.nextLine(); // clear buffer
            System.out.print("Enter switch DPID (e.g., 0000000000000001): ");
            String dpid = sc.nextLine();

            String json = "{\"dpid\": " + Long.parseLong(dpid) + "}";

            URL url = new URL(CONTROLLER_URL + "/stats/flowentry/clear");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes());
            os.flush();
            os.close();

            System.out.println("Response Code: " + conn.getResponseCode());
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();

        } catch (Exception e) {
            System.out.println("Error deleting flow rule: " + e.getMessage());
        }
    }
}
