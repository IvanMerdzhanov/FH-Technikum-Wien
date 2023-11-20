package mtcg.service;

import mtcg.models.User;
import mtcg.models.UserStats;

public class UserService {

    // Simulate a database operation that retrieves a user by username
    public User getUserByUsername(String username) {
        // TODO: Implement real database retrieval logic here
        // This is just a placeholder for demonstration purposes
        return new User(username, "hashed-password");
    }

    public boolean register(User newUser) {
        // TODO: Check if user already exists and hash password
        // For now, assume the user is registered successfully
        newUser.getUserStats().setEloScore(100); // Set initial ELO score
        return true; // Simulate user registration
    }

    public boolean login(String username, String password) {
        // TODO: Retrieve user from database and check password
        // For now, assume the login is successful
        return true; // Simulate user login
    }

    // Add methods to interact with UserStats like updating ELO score, etc.
    public boolean updateEloScore(User user, boolean win) {
        UserStats stats = user.getUserStats();
        return stats.updateEloScore(win); // Pass the win boolean to the UserStats method
    }

}
