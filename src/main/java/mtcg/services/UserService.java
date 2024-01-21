package mtcg.services;

import mtcg.models.User;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserService implements IUserService {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();

    @Override
    public User getUser(String username) {
        return users.get(username);
    }

    @Override
    public void updateUser(User user) {
        users.put(user.getUsername(), user);
    }

    @Override
    public void removeUser(User user) {
        if (user != null) {
            users.remove(user.getUsername());
        }
    }

    @Override
    public boolean isActiveSession(String token) {
        if (token == null) {
            return false;
        }
        return activeSessions.containsKey(token);
    }

    @Override
    public void removeSession(String token) {
        activeSessions.remove(token);
    }

    @Override
    public void addSession(String token, String username) {
        activeSessions.put(token, username);
    }

    @Override
    public String getUsernameForToken(String token) {
        return activeSessions.get(token);
    }

    @Override
    public String getActiveSessions() {
        return activeSessions.toString();
    }

    @Override
    public int getActiveSessionsCount() {
        return activeSessions.size();
    }

    @Override
    public void clearAllUsersAndSessions() {
        users.clear();
        activeSessions.clear();
    }

    @Override
    public void updateUsername(String oldUsername, String newUsername) {
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
