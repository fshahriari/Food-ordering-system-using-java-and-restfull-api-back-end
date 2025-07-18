package com.snappfood.server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.snappfood.controller.AdminController;
import com.snappfood.controller.UserController;
import com.snappfood.exception.*;
import com.snappfood.model.User;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
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

    private String getStatusText(int statusCode) {
        switch (statusCode) {
            case 200: return "OK";
            case 201: return "Created";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 409: return "Conflict";
            case 415: return "Unsupported Media Type";
            case 429: return "Too Many Requests";
            case 500: return "Internal Server Error";
            default: return "OK";
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> queryParams = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name()) : pair;
                String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name()) : null;
                queryParams.put(key, value);
            } catch (Exception e) {
                //ignoring malformed parameters
            }
        }
        return queryParams;
    }

    @Override
    public void run() {
        String httpResponse;
        try {
            String[] requestLines = request.split("\r\n");
            if (requestLines.length == 0) {
                throw new InvalidInputException("Invalid request");
            }

            String[] requestLine = requestLines[0].split(" ");
            if (requestLine.length < 2) {
                throw new InvalidInputException("Malformed request line");
            }
            String method = requestLine[0];
            String fullPath = requestLine[1];

            String[] pathParts = fullPath.split("\\?", 2);
            String path = pathParts[0];
            String query = pathParts.length > 1 ? pathParts[1] : "";
            Map<String, String> queryParams = parseQueryParams(query);


            Map<String, String> headers = new HashMap<>();
            String body = "";
            boolean isHeaderSection = true;
            for (int i = 1; i < requestLines.length; i++) {
                if (isHeaderSection) {
                    if (requestLines[i].isEmpty()) {
                        isHeaderSection = false;
                        continue;
                    }
                    String[] headerParts = requestLines[i].split(": ", 2);
                    if (headerParts.length == 2) {
                        headers.put(headerParts[0], headerParts[1]);
                    }
                } else {
                    body += requestLines[i];
                }
            }

            Map<String, Object> responseMap = Collections.emptyMap();
            int statusCode = 200;

            try {
                if ((method.equals("POST") || method.equals("PUT") || method.equals("PATCH")) &&
                        (headers.get("Content-Type") == null || !headers.get("Content-Type").toLowerCase().startsWith("application/json"))) {
                    throw new UnsupportedMediaTypeException("Content-Type header must be 'application/json' for POST, PUT, or PATCH requests.");
                }
                if (method.equals("GET") && body != null && !body.isEmpty()) {
                    throw new UnsupportedMediaTypeException("GET requests cannot have a message body.");
                }

                Integer userId = null;
                String token = null;
                if (headers.containsKey("Authorization")) {
                    token = headers.get("Authorization").replace("Bearer ", "");
                    userId = SessionRegistry.getUserIdFromToken(token);
                }

                String[] pathSegments = path.split("/");

                switch (pathSegments[1]) {
                    case "auth":
                        if (path.equals("/auth/register") && method.equals("POST")) {
                            User userToRegister = gson.fromJson(body, User.class);
                            responseMap = userController.handleSignup(userToRegister);
                        } else if (path.equals("/auth/login") && method.equals("POST")) {
                            Map<String, String> loginData = gson.fromJson(body, Map.class);
                            if (loginData == null || loginData.get("phone") == null) {
                                throw new InvalidInputException("Invalid phone");
                            }
                            if (loginData.get("password") == null) {
                                throw new InvalidInputException("Invalid password");
                            }
                            responseMap = userController.handleLogin(loginData.get("phone"), loginData.get("password"));
                        } else if (path.equals("/auth/profile") && method.equals("GET")) {
                            responseMap = userController.handleGetProfile(userId);
                        } else if (path.equals("/auth/profile") && method.equals("PUT")) {
                            User updatedData = gson.fromJson(body, User.class);
                            responseMap = userController.handleUpdateProfile(userId, updatedData);
                        } else if (path.equals("/auth/logout") && method.equals("POST")) {
                            responseMap = userController.handleLogout(token);
                        }
                        break;
                    default:
                        statusCode = 404;
                        responseMap = Map.of("error", "Resource not found");
                        break;
                }

            } catch (UnsupportedMediaTypeException e) {
                statusCode = 415;
                responseMap = Map.of("error", "Unsupported Media Type");
            } catch (InvalidInputException e) {
                statusCode = 400;
                responseMap = Map.of("error", e.getMessage());
            } catch (DuplicatePhoneNumberException | ConflictException e) {
                statusCode = 409;
                responseMap = Map.of("error", "Conflict occurred");
            } catch (ResourceNotFoundException e) {
                statusCode = 404;
                responseMap = Map.of("error", "Resource not found");
            } catch (UnauthorizedException e) {
                statusCode = 401;
                responseMap = Map.of("error", "Unauthorized request");
            } catch (ForbiddenException e) {
                statusCode = 403;
                responseMap = Map.of("error", "Forbidden request");
            } catch (TooManyRequestsException e) {
                statusCode = 429;
                responseMap = Map.of("error", "Too many requests");
            } catch (JsonSyntaxException e) {
                statusCode = 400;
                responseMap = Map.of("error", "Invalid JSON format.");
            } catch (SQLException e) {
                statusCode = 500;
                responseMap = Map.of("error", "Internal server error");
                e.printStackTrace();
            }
            catch (InternalServerErrorException e) {
                statusCode = 500;
                responseMap = Map.of("error", "Internal server error");
                e.printStackTrace();
            } catch (Exception e) {
                statusCode = 500;
                responseMap = Map.of("error", "Internal server error");
                e.printStackTrace();
            }

            String statusText = getStatusText(statusCode);
            String jsonResponse = gson.toJson(responseMap);
            httpResponse = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + jsonResponse.length() + "\r\n" +
                    "\r\n" +
                    jsonResponse;

        } catch (Exception e) {
            String errorJson = gson.toJson(Map.of("error", "Error processing request."));
            httpResponse = "HTTP/1.1 500 Internal Server Error\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + errorJson.length() + "\r\n" +
                    "\r\n" +
                    errorJson;
            e.printStackTrace();
        }

        System.out.println("--- SERVER RESPONSE ---");
        System.out.println(httpResponse);
        System.out.println("-----------------------");

        try {
            ByteBuffer responseBuffer = ByteBuffer.wrap(httpResponse.getBytes());
            clientChannel.write(responseBuffer);
        } catch (IOException e) {
            System.err.println("Error sending response to client: " + e.getMessage());
        } finally {
            try {
                clientChannel.close();
            } catch (IOException e) {
                System.err.println("Could not close client channel cleanly: " + e.getMessage());
            }
        }
    }
}
