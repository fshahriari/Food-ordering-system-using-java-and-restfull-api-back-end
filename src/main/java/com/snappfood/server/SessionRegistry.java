package com.snappfood.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionRegistry {

    private static final Map<String, Integer> activeSessions = new ConcurrentHashMap<>();

    /**
     * Creates a new session for a user.
     *
     * @param userId The ID of the user.
     * @return The session token.
     */
    public static String createSession(int userId) {
        String token = UUID.randomUUID().toString();
        activeSessions.put(token, userId);
        return token;
    }

    /**
     * Retrieves the user ID associated with a session token.
     *
     * @param token The session token.
     * @return The user ID, or null if the token is invalid.
     */
    public static Integer getUserIdFromToken(String token) {
        return activeSessions.get(token);
    }
}
