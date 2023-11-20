package mtcg.controller;

import mtcg.models.User;
import mtcg.service.UserService;

public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Method to handle user registration
    public String registerUser(String username, String password) {
        User newUser = new User(username, password);
        boolean success = userService.register(newUser);

        if (success) {
            return "User registered successfully!";
        } else {
            return "Registration failed.";
        }
    }

    // Method to handle user login
    public String loginUser(String username, String password) {
        boolean success = userService.login(username, password);

        if (success) {
            return "User logged in successfully!";
        } else {
            return "Login failed.";
        }
    }

    // Add more methods for other user-related operations
}
