package com.snappfood.server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.snappfood.controller.*;
import com.snappfood.exception.*;
import com.snappfood.model.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestHandler implements Runnable {

    private final String request;
    private final SocketChannel clientChannel;
    private final UserController userController;
    private final AdminController adminController;
    private final RestaurantController restaurantController;
    private final OrderController orderController;
    private final CustomerController customerController;
    private final CourierController courierController;
    private final WalletController walletController;
    private final Gson gson;

    public RequestHandler(String request, SocketChannel clientChannel) {
        this.request = request;
        this.clientChannel = clientChannel;
        this.userController = new UserController();
        this.adminController = new AdminController();
        this.restaurantController = new RestaurantController();
        this.orderController = new OrderController();
        this.customerController = new CustomerController();
        this.courierController = new CourierController();
        this.walletController = new WalletController();
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
            // Log the incoming request
            if (request == null || request.trim().isEmpty()) {
                return;
            }

            String[] requestParts = request.split("\r\n\r\n", 2);
            String headerPart = requestParts[0];
            String body = (requestParts.length > 1) ? requestParts[1] : "";

            String[] requestLines = headerPart.split("\r\n");
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

            System.out.println("HTTP Method: " + method);
            System.out.println("Endpoint: " + path);
            System.out.println("Request Body: " + body);

            Map<String, String> headers = new HashMap<>();
            for (int i = 1; i < requestLines.length; i++) {
                String[] headerParts = requestLines[i].split(": ", 2);
                if (headerParts.length == 2) {
                    headers.put(headerParts[0], headerParts[1]);
                }
            }

            Map<String, Object> responseMap = Collections.emptyMap();
            int statusCode = 200;

            try {
                if ((method.equals("POST") || method.equals("PUT") || method.equals("PATCH"))) {
                    if (!path.equals("/auth/logout")) {
                        if (headers.get("Content-Type") == null || !headers.get("Content-Type").toLowerCase().startsWith("application/json")) {
                            throw new UnsupportedMediaTypeException("Content-Type header must be 'application/json' for this request.");
                        }
                    }
                }
                if ((method.equals("GET") || method.equals("DELETE")) && body != null && !body.isEmpty()) {
                    throw new UnsupportedMediaTypeException("GET requests cannot have a message body.");
                }

                Integer userId = null;
                String token = null;
                if (headers.containsKey("Authorization")) {
                    token = headers.get("Authorization").replace("Bearer ", "");
                    System.out.println("Token: " + token);
                    if (token.isEmpty()) {
                        throw new UnauthorizedException("Authentication token is required.");
                    }
                    userId = SessionRegistry.getUserIdFromToken(token);
                }

                String[] pathSegments = path.split("/");
                if (pathSegments.length < 2) {
                    throw new ResourceNotFoundException("Not Found");
                }

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
                            responseMap = userController.handleUpdateProfile(userId, body);
                        } else if (path.equals("/auth/logout") && method.equals("POST")) {
                            responseMap = userController.handleLogout(token);
                        }
                        break;

                    case "restaurants":
                        if (path.equals("/restaurants") && method.equals("POST")) {
                            Restaurant newRestaurant = gson.fromJson(body, Restaurant.class);
                            responseMap = restaurantController.handleCreateRestaurant(newRestaurant, userId);
                        } else if (path.equals("/restaurants/mine") && method.equals("GET")) {
                            responseMap = restaurantController.handleGetMyRestaurants(userId);
                        } else if (pathSegments.length == 3 && method.equals("PUT")) {
                            Integer restaurantId = Integer.parseInt(pathSegments[2]);
                            Restaurant updateData = gson.fromJson(body, Restaurant.class);
                            responseMap = restaurantController.handleUpdateRestaurant(restaurantId, updateData, userId);
                        } else if (pathSegments.length == 4 && pathSegments[3].equals("items") && method.equals("GET")) {
                            if (userId == null) {
                                throw new UnauthorizedException("Authentication required. Please log in.");
                            }
                            int restaurantId = Integer.parseInt(pathSegments[2]);
                            responseMap = restaurantController.handleGetMasterFoodList(userId, restaurantId);
                        } else if (pathSegments.length == 4 && pathSegments[3].equals("item") && method.equals("POST")) {
                            if (userId == null) {
                                throw new UnauthorizedException("Authentication required. Please log in.");
                            }
                            int restaurantId = Integer.parseInt(path.split("/")[2]);
                            Food food = gson.fromJson(body, Food.class);
                            responseMap = restaurantController.handleAddFoodItemToMasterList(restaurantId, userId, food);
                        } else if (pathSegments.length == 5 && pathSegments[3].equals("item") && method.equals("PUT")) {
                            Integer restaurantId = Integer.parseInt(pathSegments[2]);
                            Integer itemId = Integer.parseInt(pathSegments[4]);
                            Food updatedFood = gson.fromJson(body, Food.class);
                            responseMap = restaurantController.handleUpdateMasterFoodItem(restaurantId, itemId, userId, updatedFood);
                        } else if (pathSegments.length == 5 && pathSegments[3].equals("item") && method.equals("DELETE")) {
                            Integer restaurantId = Integer.parseInt(pathSegments[2]);
                            Integer itemId = Integer.parseInt(pathSegments[4]);
                            responseMap = restaurantController.handleDeleteMasterFoodItem(restaurantId, itemId, userId);
                        }
                        else if (pathSegments.length == 4 && pathSegments[3].equals("menu") && method.equals("POST")) {
                            if (userId == null) throw new UnauthorizedException("Authentication is required.");
                            Integer restaurantId = Integer.parseInt(pathSegments[2]);
                            Map<String, String> requestBody = gson.fromJson(body, Map.class);
                            if (requestBody == null) {
                                throw new InvalidInputException("Request body is missing.");
                            }
                            String title = requestBody.get("title");
                            responseMap = restaurantController.handleCreateMenu(restaurantId, userId, title);
                        } else if (pathSegments.length == 5 && pathSegments[3].equals("menu") && method.equals("DELETE")) {
                            Integer restaurantId = Integer.parseInt(pathSegments[2]);
                            String title = pathSegments[4];
                            responseMap = restaurantController.handleDeleteTitledMenu(restaurantId, userId, title);
                        } else if (pathSegments.length == 5 && pathSegments[3].equals("menu") && method.equals("PUT")) {
                            Integer restaurantId = Integer.parseInt(pathSegments[2]);
                            String title = pathSegments[4];
                            Type type = new TypeToken<Map<String, Double>>(){}.getType();
                            Map<String, Double> requestBody = gson.fromJson(body, type);
                            Integer itemId = requestBody != null ? requestBody.get("item_id").intValue() : null;
                            responseMap = restaurantController.handleAddItemToTitledMenu(restaurantId, userId, title, itemId);
                        } else if (pathSegments.length == 6 && pathSegments[3].equals("menu") && method.equals("DELETE")) {
                            Integer restaurantId = Integer.parseInt(pathSegments[2]);
                            String title = pathSegments[4];
                            Integer itemId = Integer.parseInt(pathSegments[5]);
                            responseMap = restaurantController.handleRemoveItemFromTitledMenu(restaurantId, userId, title, itemId);
                        } else if (pathSegments.length == 4 && pathSegments[3].equals("orders") && method.equals("GET")) {
                            int restaurantId = Integer.parseInt(pathSegments[2]);
                            responseMap = restaurantController.handleGetRestaurantOrders(userId, restaurantId, queryParams);
                        } else if (pathSegments.length == 5 && pathSegments[3].equals("orders") && method.equals("PATCH")) {
                            int restaurantId = Integer.parseInt(pathSegments[2]);
                            int orderId = Integer.parseInt(pathSegments[4]);
                            Map<String, String> requestBody = gson.fromJson(body, Map.class);
                            responseMap = restaurantController.handleUpdateOrderStatus(userId, restaurantId, orderId, requestBody);
                        }
                        break;

                    case "orders":
                        if (path.equals("/orders") && method.equals("POST")) {
                            if (userId == null) throw new UnauthorizedException("Authentication is required.");

                            Type orderRequestType = new TypeToken<Map<String, Object>>() {}.getType();
                            Map<String, Object> orderRequest = gson.fromJson(body, orderRequestType);

                            Order order = new Order();
                            order.setDeliveryAddress((String) orderRequest.get("delivery_address"));

                            // Treat all numbers as the generic Number class
                            order.setRestaurantId(((Number) orderRequest.get("vendor_id")).intValue());
                            if (orderRequest.get("coupon_id") != null) {
                                order.setCouponId(((Number) orderRequest.get("coupon_id")).intValue());
                            }

                            List<Map<String, Object>> itemsList = (List<Map<String, Object>>) orderRequest.get("items");
                            Map<Integer, Integer> itemsMap = new HashMap<>();
                            for (Map<String, Object> item : itemsList) {
                                itemsMap.put(((Number) item.get("item_id")).intValue(), ((Number) item.get("quantity")).intValue());
                            }

                            order.setItems(itemsMap);
                            responseMap = orderController.handleCreateOrder(order, userId);
                        } else if (pathSegments.length == 3 && method.equals("GET")) {
                            int orderId = Integer.parseInt(pathSegments[2]);
                            responseMap = orderController.handleGetOrderDetails(userId, orderId);
                        } else if (path.equals("/orders/history") && method.equals("GET")) {
                            responseMap = orderController.handleGetOrderHistory(userId, queryParams);
                        }
                        break;
                    case "admin":
                        if (path.equals("/admin/users") && method.equals("GET")) {
                            responseMap = adminController.handleListAllUsers(userId);
                        } else if (path.equals("/admin/orders") && method.equals("GET")) {
                            responseMap = adminController.handleGetAllOrders(userId, queryParams);
                        } else if (path.equals("/admin/transactions") && method.equals("GET")) {
                            responseMap = adminController.handleGetAllTransactions(userId, queryParams);
                        } else if (path.equals("/admin/pending-users") && method.equals("GET")) {
                            responseMap = adminController.handleGetPendingUsers(userId);
                        }  else if (path.equals("/admin/pending-users") && method.equals("PUT")) {
                            Type listType = new TypeToken<List<UserStatusUpdate>>() {}.getType();
                            List<UserStatusUpdate> userUpdates = gson.fromJson(body, listType);
                            responseMap = adminController.handleUpdatePendingUsers(userId, userUpdates);
                        } else if (path.equals("/admin/pending-orders") && method.equals("GET")) {
                            responseMap = adminController.handleGetPendingOrders(userId);
                        } else if (path.equals("/admin/pending-orders") && method.equals("PUT")) {
                            Type listType = new TypeToken<List<OrderStatusUpdate>>() {}.getType();
                            List<OrderStatusUpdate> orderUpdates = gson.fromJson(body, listType);
                            responseMap = adminController.handleUpdatePendingOrders(userId, orderUpdates);
                        } else if (path.equals("/admin/pending-restaurants") && method.equals("GET")) {
                            responseMap = adminController.handleGetPendingRestaurants(userId);
                        } else if (path.equals("/admin/pending-restaurants") && method.equals("PUT")) {
                            Type listType = new TypeToken<List<RestaurantStatusUpdate>>() {}.getType();
                            List<RestaurantStatusUpdate> restaurantUpdates = gson.fromJson(body, listType);
                            responseMap = adminController.handleUpdatePendingRestaurants(userId, restaurantUpdates);
                        }
                        break;
                    case "vendors":
                        if (path.equals("/vendors") && method.equals("POST")) {
                            Type type = new TypeToken<Map<String, Object>>(){}.getType();
                            Map<String, Object> filters = gson.fromJson(body, type);
                            responseMap = customerController.handleListVendors(userId, filters);
                        } else if (pathSegments.length == 3 && method.equals("GET")) {
                            int restaurantId = Integer.parseInt(pathSegments[2]);
                            responseMap = customerController.handleGetVendorDetails(userId, restaurantId);
                        }
                        break;
                    case "items":
                        if (path.equals("/items") && method.equals("POST")) {
                            Type type = new TypeToken<Map<String, Object>>(){}.getType();
                            Map<String, Object> filters = gson.fromJson(body, type);
                            responseMap = customerController.handleListItems(userId, filters);
                        } else if (pathSegments.length == 3 && method.equals("GET")) {
                            int itemId = Integer.parseInt(pathSegments[2]);
                            responseMap = customerController.handleGetItemDetails(userId, itemId);
                        }
                        break;
                    case "favorites":
                        if (pathSegments.length == 3 && method.equals("PUT")) {
                            int restaurantId = Integer.parseInt(pathSegments[2]);
                            responseMap = customerController.handleAddFavoriteRestaurant(userId, restaurantId);
                        } else if (path.equals("/favorites") && method.equals("GET")) {
                            responseMap = customerController.handleGetFavoriteRestaurants(userId);
                        } else if (pathSegments.length == 3 && method.equals("DELETE")) {
                            int restaurantId = Integer.parseInt(pathSegments[2]);
                            responseMap = customerController.handleRemoveFavoriteRestaurant(userId, restaurantId);
                        }
                        break;
                    case "ratings":
                        if (path.equals("/ratings") && method.equals("POST")) {
                            Rating rating = gson.fromJson(body, Rating.class);
                            responseMap = customerController.handleSubmitRating(userId, rating);
                        } else if (pathSegments.length == 3 && method.equals("GET")) {
                            int orderId = Integer.parseInt(pathSegments[2]);
                            responseMap = customerController.handleGetRatingByOrderId(userId, orderId);
                        }
                        break;
                    case "deliveries":
                        if (path.equals("/deliveries/available") && method.equals("GET")) {
                            responseMap = courierController.handleGetAvailableDeliveries(userId);
                        } else if (path.equals("/deliveries/history") && method.equals("GET")) {
                            responseMap = courierController.handleGetDeliveryHistory(userId, queryParams);
                        } else if (pathSegments.length == 3 && method.equals("PATCH")) {
                            int orderId = Integer.parseInt(pathSegments[2]);
                            Map<String, String> requestBody = gson.fromJson(body, Map.class);
                            responseMap = courierController.handleUpdateDeliveryStatus(userId, orderId, requestBody);
                        }
                        break;
                    case "wallet":
                        if (path.equals("/wallet/top-up") && method.equals("POST")) {
                            Type type = new TypeToken<Map<String, Double>>(){}.getType();
                            Map<String, Double> requestBody = gson.fromJson(body, type);
                            responseMap = walletController.handleTopUp(userId, requestBody);
                        }
                        break;
                    case "payment":
                        if (path.equals("/payment/online") && method.equals("POST")) {
                            Type type = new TypeToken<Map<String, Object>>(){}.getType();
                            Map<String, Object> requestBody = gson.fromJson(body, type);
                            responseMap = walletController.handlePayment(userId, requestBody);
                        }
                        break;
                    case "transactions":
                        if (path.equals("/transactions") && method.equals("GET")) {
                            responseMap = walletController.handleGetTransactionHistory(userId);
                        }
                        break;
                    default:
                        statusCode = 404;
                        responseMap = Map.of("error", "Not Found");
                        break;
                }
                if (responseMap.containsKey("status")) {
                    statusCode = (int) responseMap.get("status");
                }

            } catch (NumberFormatException e) {
                statusCode = 400;
                responseMap = Map.of("error", "Invalid ID format in URL.");
            } catch (UnsupportedMediaTypeException | JsonSyntaxException e) {
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
            } catch (SQLException e) {
                statusCode = 500;
                responseMap = Map.of("error", "A database error occurred.");
                e.printStackTrace();
            }
            catch (InternalServerErrorException e) {
                statusCode = 500;
                responseMap = Map.of("error", "An internal server error occurred.");
                e.printStackTrace();
            } catch (Exception e) {
                statusCode = 500;
                responseMap = Map.of("error", "An unexpected error occurred.");
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