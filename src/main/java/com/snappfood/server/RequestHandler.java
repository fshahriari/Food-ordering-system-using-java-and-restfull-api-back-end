package com.snappfood.server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.snappfood.controller.AdminController;
import com.snappfood.controller.UserController;
import com.snappfood.exception.DuplicatePhoneNumberException;
import com.snappfood.exception.InvalidInputException;
import com.snappfood.model.User;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RequestHandler implements Runnable {

    private final String request;
    private final SocketChannel clientChannel;
    private final UserController userController;
    private final AdminController adminController;
    private final Gson gson;

    public RequestHandler(String request, SocketChannel clientChannel) {
        this.request = request;
        this.clientChannel = clientChannel;
        this.userController = new UserController();
        this.adminController = new AdminController();
        this.gson = new Gson();
    }

    @Override
    public void run() {
        String httpResponse;
        try {
            // 1. Parse the incoming HTTP request
            String[] requestLines = request.split("\r\n");
            String[] requestLine = requestLines[0].split(" ");
            String method = requestLine[0];
            String path = requestLine[1];

            Map<String, String> headers = new HashMap<>();
            String body = "";
            boolean isHeaderSection = true;
            for (int i = 1; i < requestLines.length; i++) {
                if (isHeaderSection) {
                    if (requestLines[i].isEmpty()) {
                        isHeaderSection = false;
                        continue;
                    }
                    String[] headerParts = requestLines[i].split(": ");
                    if (headerParts.length == 2) {
                        headers.put(headerParts[0], headerParts[1]);
                    }
                } else {
                    body += requestLines[i];
                }
            }

            // 2. Prepare for response
            Map<String, Object> responseMap = Collections.emptyMap();
            int statusCode = 200;

            // 3. Route the request based on the path
            try {
                // Check for authorization token if required by the endpoint
                Integer userId = null;
                if (headers.containsKey("Authorization")) {
                    String token = headers.get("Authorization").replace("Bearer ", "");
                    userId = SessionRegistry.getUserIdFromToken(token);
                }

                String[] pathParts = path.split("/");

                switch (pathParts[1]) {
                    case "auth":
                        if (path.equals("/auth/register") && method.equals("POST")) {
                            User userToRegister = gson.fromJson(body, User.class);
                            responseMap = userController.handleSignup(userToRegister);
                        } else if (path.equals("/auth/login") && method.equals("POST")) {
                            Map<String, String> loginData = gson.fromJson(body, Map.class);
                            responseMap = userController.handleLogin(loginData.get("phone"), loginData.get("password"));
                        }
                        // ... other auth routes
                        break;

                    // ... other cases

                    default:
                        statusCode = 404;
                        responseMap = Map.of("error", "Not Found");
                        break;
                }

                // --- NEW ERROR HANDLING ---
            } catch (InvalidInputException e) {
                statusCode = 400; // Bad Request
                responseMap = Map.of("error", e.getMessage());
            } catch (DuplicatePhoneNumberException e) {
                statusCode = 409; // Conflict
                responseMap = Map.of("error", e.getMessage());
            } catch (JsonSyntaxException e) {
                statusCode = 400; // Bad Request
                responseMap = Map.of("error", "Invalid JSON format.");
            } catch (Exception e) {
                statusCode = 500; // Internal Server Error
                responseMap = Map.of("error", "An unexpected internal server error occurred.");
                e.printStackTrace(); // Log the full error for debugging
            }

            // 4. Create the HTTP response
            String jsonResponse = gson.toJson(responseMap);
            httpResponse = "HTTP/1.1 " + statusCode + " OK\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + jsonResponse.length() + "\r\n" +
                    "\r\n" +
                    jsonResponse;

        } catch (Exception e) {
            // Catch-all for parsing or initial setup errors
            String errorJson = gson.toJson(Map.of("error", "Error processing request."));
            httpResponse = "HTTP/1.1 500 Internal Server Error\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + errorJson.length() + "\r\n" +
                    "\r\n" +
                    errorJson;
            e.printStackTrace();
        }

        // 5. Send the response
        try {
            ByteBuffer responseBuffer = ByteBuffer.wrap(httpResponse.getBytes());
            clientChannel.write(responseBuffer);
        } catch (IOException e) {
            System.err.println("Error sending response to client: " + e.getMessage());
        } finally {
            try {
                clientChannel.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
