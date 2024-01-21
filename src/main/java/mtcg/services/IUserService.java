package mtcg.services;

import mtcg.models.User;

public interface IUserService {
    User getUser(String username);
    void updateUser(User user);
    void removeUser(User user);
    boolean isActiveSession(String token);
    void removeSession(String token);
    void addSession(String token, String username);
    String getUsernameForToken(String token);
    String getActiveSessions();
    int getActiveSessionsCount();
    void clearAllUsersAndSessions();
    void updateUsername(String oldUsername, String newUsername);
}
