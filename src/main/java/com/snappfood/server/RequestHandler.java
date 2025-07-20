package com.snappfood.server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.snappfood.controller.AdminController;
import com.snappfood.controller.RestaurantController;
import com.snappfood.controller.UserController;
import com.snappfood.exception.*;
import com.snappfood.model.Food;
import com.snappfood.model.Restaurant;
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
    private final RestaurantController restaurantController; // Added RestaurantController
    private final Gson gson;

    public RequestHandler(String request, SocketChannel clientChannel) {
        this.request = request;
        this.clientChannel = clientChannel;
        this.userController = new UserController();
        this.adminController = new AdminController();
        this.restaurantController = new RestaurantController(); // Initialize it
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
                if ((method.equals("POST") || method.equals("PUT") || method.equals("PATCH"))) {
                    // Only enforce Content-Type for POST/PUT/PATCH requests that are NOT for logging out
                    if (!path.equals("/auth/logout")) {
                        if (headers.get("Content-Type") == null || !headers.get("Content-Type").toLowerCase().startsWith("application/json")) {
                            throw new UnsupportedMediaTypeException("Content-Type header must be 'application/json' for this request.");
                        }
                    }
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

                    case "restaurants":
                        if (path.equals("/restaurants") && method.equals("POST")) {
                            Restaurant newRestaurant = gson.fromJson(body, Restaurant.class);
                            responseMap = restaurantController.handleCreateRestaurant(newRestaurant, userId);
                            if (responseMap.containsKey("status")) {
                                statusCode = (int) responseMap.get("status");
                            }
                        } else if (path.equals("/restaurants/mine") && method.equals("GET")) {
                            responseMap = restaurantController.handleGetMyRestaurants(userId);
                            if (responseMap.containsKey("status")) {
                                statusCode = (int) responseMap.get("status");
                            }
                        }else if (pathSegments.length == 3 && method.equals("PUT")) {
                            try {
                                Integer restaurantId = Integer.parseInt(pathSegments[2]);
                                Restaurant updateData = gson.fromJson(body, Restaurant.class);
                                responseMap = restaurantController.handleUpdateRestaurant(restaurantId, updateData, userId);
                                statusCode = (int) responseMap.get("status");
                            } catch (NumberFormatException e) {
                                throw new ResourceNotFoundException("Invalid restaurant ID format.");
                            }
                        } else if (pathSegments.length == 4 && pathSegments[3].equals("item") && method.equals("POST")) {
                            try {
                                Integer restaurantId = Integer.parseInt(pathSegments[2]);
                                Food newFood = gson.fromJson(body, Food.class);
                                responseMap = restaurantController.handleAddFoodItem(restaurantId, newFood, userId);
                                statusCode = (int) responseMap.get("status");
                            } catch (NumberFormatException e) {
                                statusCode = 404;
                                responseMap = Map.of("error", "Resource not found: Invalid restaurant ID format.");
                            }
                        } else if (pathSegments.length == 5 && pathSegments[3].equals("item") && method.equals("PUT")) {
                            try {
                                Integer restaurantId = Integer.parseInt(pathSegments[2]);
                                Integer itemId = Integer.parseInt(pathSegments[4]);
                                Food updatedFood = gson.fromJson(body, Food.class);
                                responseMap = restaurantController.handleUpdateFoodItem(restaurantId, itemId, updatedFood, userId);
                                statusCode = (int) responseMap.get("status");
                            } catch (NumberFormatException e) {
                                statusCode = 404;
                                responseMap = Map.of("error", "Resource not found: Invalid restaurant or item ID format.");
                            }
                        } else if (pathSegments.length == 5 && pathSegments[3].equals("item") && method.equals("DELETE")) {
                            try {
                                Integer restaurantId = Integer.parseInt(pathSegments[2]);
                                Integer itemId = Integer.parseInt(pathSegments[4]);
                                responseMap = restaurantController.handleDeleteFoodItem(restaurantId, itemId, userId);
                                statusCode = (int) responseMap.get("status");
                            } catch (NumberFormatException e) {
                                statusCode = 404;
                                responseMap = Map.of("error", "Resource not found: Invalid restaurant or item ID format.");
                            }
                        }
                        break;

                    default:
                        statusCode = 404;
                        responseMap = Map.of("error", "not found");
                        break;
                }

            } catch (UnsupportedMediaTypeException e) {
                statusCode = 415;
                responseMap = Map.of("error", e.getMessage());
            } catch (InvalidInputException e) {
                statusCode = 400;
                responseMap = Map.of("error", e.getMessage());
            } catch (DuplicatePhoneNumberException | ConflictException e) {
                statusCode = 409;
                responseMap = Map.of("error", e.getMessage());
            } catch (ResourceNotFoundException e) {
                statusCode = 404;
                responseMap = Map.of("error", e.getMessage());
            } catch (UnauthorizedException e) {
                statusCode = 401;
                responseMap = Map.of("error", e.getMessage());
            } catch (ForbiddenException e) {
                statusCode = 403;
                responseMap = Map.of("error", e.getMessage());
            } catch (TooManyRequestsException e) {
                statusCode = 429;
                responseMap = Map.of("error", e.getMessage());
            } catch (JsonSyntaxException e) {
                statusCode = 400;
                responseMap = Map.of("error", e.getMessage());
            } catch (SQLException e) {
                statusCode = 500;
                responseMap = Map.of("error", e.getMessage());
                e.printStackTrace();
            }
            catch (InternalServerErrorException e) {
                statusCode = 500;
                responseMap = Map.of("error", e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                statusCode = 500;
                responseMap = Map.of("error", e.getMessage());
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
