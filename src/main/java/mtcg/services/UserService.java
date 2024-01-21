package mtcg.services;

import mtcg.models.User;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserService implements IUserService {
    private static UserService instance;
    private UserService() {}

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();

    @Override
    public User getUser(String username) {
        System.out.println("UserService: Getting user with username - " + username);
        return users.get(username);
    }

    @Override
    public void updateUser(User user) {
        System.out.println("UserService: Updating user - " + user.getUsername());
        users.put(user.getUsername(), user);
    }

    @Override
    public void removeUser(User user) {
        if (user != null) {
            System.out.println("UserService: Removing user - " + user.getUsername());
            users.remove(user.getUsername());
        }
    }

    @Override
    public boolean isActiveSession(String token) {
        System.out.println("UserService: Checking if token is active - " + token);
        return activeSessions.containsKey(token);
    }

    @Override
    public void removeSession(String token) {
        System.out.println("UserService: Removing session with token - " + token);
        activeSessions.remove(token);
    }

    @Override
    public void addSession(String token, String username) {
        System.out.println("UserService: Adding session. Token: " + token + ", Username: " + username);
        activeSessions.put(token, username);
    }

    @Override
    public String getUsernameForToken(String token) {
        System.out.println("UserService: Getting username for token - " + token);
        return activeSessions.get(token);
    }

    @Override
    public String getActiveSessions() {
        System.out.println("UserService: Active sessions - " + activeSessions.toString());
        return activeSessions.toString();
    }

    @Override
    public int getActiveSessionsCount() {
        System.out.println("UserService: Active session count - " + activeSessions.size());
        return activeSessions.size();
    }

    @Override
    public void clearAllUsersAndSessions() {
        System.out.println("UserService: Clearing all users and sessions");
        users.clear();
        activeSessions.clear();
    }

    @Override
    public void updateUsername(String oldUsername, String newUsername) {
        System.out.println("UserService: Updating username from " + oldUsername + " to " + newUsername);
        User user = users.get(oldUsername);
        if (user != null) {
            user.setUsername(newUsername);
            users.remove(oldUsername);
            users.put(newUsername, user);
            activeSessions.entrySet().forEach(entry -> {
                if (entry.getValue().equals(oldUsername)) {
                    entry.setValue(newUsername);
                }
            });
        }
    }
}
