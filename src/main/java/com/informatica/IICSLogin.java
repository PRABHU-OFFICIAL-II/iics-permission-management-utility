package com.informatica;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class IICSLogin {

    private static final String LOGIN_URL = "https://dm-us.informaticacloud.com/saas/public/core/v3/login";

    // ANSI escape codes for colored text
    private static final String RESET_COLOR = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";

    public static void main(String[] args) {

        System.out.println(BLUE + "===== IICS Login =====" + RESET_COLOR);

        IICSLogin iicsLogin = new IICSLogin();
        iicsLogin.login();
    }

    public void login() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter your username: ");
        String username = scanner.nextLine();

        System.out.print("Enter your password: ");
        String password = scanner.nextLine();

        try {
            // Create JSON payload
            String jsonPayload = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";

            // Send POST request
            HttpURLConnection connection = (HttpURLConnection) new URL(LOGIN_URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            System.out.println(YELLOW + "Logging in..." + RESET_COLOR);

            // Write JSON payload to request body
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes("utf-8"));
            }

            // Get response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse the response to get baseApiUrl and sessionId
                String responseStr = response.toString();
                String baseApiUrl = responseStr.split("\"baseApiUrl\":\"")[1].split("\"")[0];
                String sessionId = responseStr.split("\"sessionId\":\"")[1].split("\"")[0];

                // Display results
                System.out.println(GREEN + "Login successful!" + RESET_COLOR);
                System.out.println(BLUE + "Base API URL: " + RESET_COLOR + baseApiUrl);
                System.out.println(BLUE + "Session ID: " + RESET_COLOR + sessionId);

                // Project Name and Folder Name
                System.out.print(CYAN + "Enter project name: " + RESET_COLOR);
                String projectName = scanner.nextLine();

                System.out.print(CYAN + "Enter folder name: " + RESET_COLOR);
                String folderName = scanner.nextLine();

                // Make GET request to fetch objects
                List<String> objectIds = fetchObjects(baseApiUrl, sessionId, projectName, folderName);

                // Delete permissions for each object ID
                if (!objectIds.isEmpty()) {
                    deletePermissionsForObjects(baseApiUrl, sessionId, objectIds);
                } else {
                    System.out.println("No objects found.");
                }
            } else {
                System.out.println(RED + "Login failed with response code: " + responseCode + RESET_COLOR);
            }
        } catch (Exception e) {
            System.out.println(RED + "Error occurred: " + e.getMessage() + RESET_COLOR);
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    private List<String> fetchObjects(String baseApiUrl, String sessionId, String projectName, String folderName) {
        List<String> objectIds = new ArrayList<>();
        try {

            System.out.println(YELLOW + "Fetching object IDs..." + RESET_COLOR);

            // Construct the GET request URL
            String apiUrl = baseApiUrl + "/public/core/v3/objects?q=location%3D%3D%27" + projectName + "%2F"
                    + folderName + "%27";

            // Send GET request
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("INFA-SESSION-ID", sessionId);

            // Get response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Extracting object IDs by manually finding and parsing object IDs in the JSON
                String responseStr = response.toString();
                int index = 0;

                while ((index = responseStr.indexOf("\"id\":\"", index)) != -1) {
                    index += 6; // Move to the start of the ID value
                    String id = responseStr.substring(index, responseStr.indexOf("\"", index));
                    objectIds.add(id);
                }

                // Display the fetched object IDs
                System.out.println(GREEN + "Fetched Object IDs: " + objectIds + RESET_COLOR);
            } else {
                System.out.println(RED + "Failed to fetch objects with response code: " + responseCode + RESET_COLOR);
            }
        } catch (Exception e) {
            System.out.println(RED + "Error occurred while fetching objects: " + e.getMessage() + RESET_COLOR);
            e.printStackTrace();
        }
        return objectIds;
    }

    private void deletePermissionsForObjects(String baseApiUrl, String sessionId, List<String> objectIds) {
        for (String objectId : objectIds) {
            try {

                System.out.println(YELLOW + "Deleting permissions for Object ID: " + objectId + RESET_COLOR);

                // Construct the DELETE request URL
                String apiUrl = baseApiUrl + "/public/core/v3/objects/" + objectId + "/permissions";

                // Send DELETE request
                HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("INFA-SESSION-ID", sessionId);

                // Get response
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    System.out.println(
                            GREEN + "Successfully deleted permissions for Object ID: " + objectId + RESET_COLOR);
                } else {
                    System.out.println(RED + "Failed to delete permissions for Object ID: " + objectId
                            + " with response code: " + responseCode + RESET_COLOR);
                }
            } catch (Exception e) {
                System.out.println(
                        RED + "Error occurred while deleting permissions for Object ID: " + objectId + RESET_COLOR);
                e.printStackTrace();
            }
        }
    }
}
