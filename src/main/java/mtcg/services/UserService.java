package mtcg.services;

import mtcg.models.User;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {
    private static final Map<String, User> users = new ConcurrentHashMap<>();
    private static final Map<String, String> activeSessions = new ConcurrentHashMap<>();

    public static User getUser(String username) {
        return users.get(username);
    }

    public static void updateUser(User user) {
        users.put(user.getUsername(), user);
    }
    public static void removeUser(User user) {
        if (user != null) {
            users.remove(user.getUsername());
        }
    }
    public static boolean isActiveSession(String token) {
        return activeSessions.containsKey(token);
    }

    public static void removeSession(String token) {
        activeSessions.remove(token);
    }

    public static void addSession(String token, String username) {
        activeSessions.put(token, username);
    }

    public static String getUsernameForToken(String token) {
        return activeSessions.get(token);
    }
    public static int getActiveSessionsCount() {
        return activeSessions.size();
    }
    public static void clearAllUsersAndSessions() {
        users.clear();
        activeSessions.clear();
    }

}

