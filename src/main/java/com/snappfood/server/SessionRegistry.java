package com.snappfood.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionRegistry {

    private static final Map<String, Integer> activeSessions = new ConcurrentHashMap<>();
    private static final Map<Integer, String> userIdToToken = new ConcurrentHashMap<>();

    /**
     * Creates a new session for a user. If the user already has an active session,
     * the old session is invalidated and replaced with the new one.
     *
     * @param userId The ID of the user.
     * @return The new session token.
     */
    public static String createSession(int userId) {
        // If user already has a session, remove the old one first.
        if (userIdToToken.containsKey(userId)) {
            String oldToken = userIdToToken.get(userId);
            activeSessions.remove(oldToken);
        }

        // Create the new session
        String token = UUID.randomUUID().toString();
        activeSessions.put(token, userId);
        userIdToToken.put(userId, token);
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

    /**
     * Checks if a user currently has an active session.
     *
     * @param userId The ID of the user to check.
     * @return true if the user has an active session, false otherwise.
     */
    public static boolean isUserActive(int userId) {
        return userIdToToken.containsKey(userId);
    }
}