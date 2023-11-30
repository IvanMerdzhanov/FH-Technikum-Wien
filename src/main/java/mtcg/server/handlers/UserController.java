package mtcg.server.handlers;

import mtcg.models.User;
import mtcg.server.services.UserService;
import mtcg.server.http.*;

public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public HttpResponse handleRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        switch (request.getUri()) {
            case "/register":
                return handleRegister(request, response);
            case "/login":
                return handleLogin(request, response);
            // add more cases for other user-related endpoints
            default:
                response.setStatus(HttpStatus.NOT_FOUND);
                response.setBody("Not Found");
                return response;
        }
    }

    private HttpResponse handleRegister(HttpRequest request, HttpResponse response) {
        // Extract registration details from request (mocked for now)
        String username = "exampleUser";
        String password = "examplePassword";

        // Call userService.registerUser() (mocked response)
        boolean isRegistered = userService.registerUser(username, password);

        if (isRegistered) {
            response.setStatus(HttpStatus.OK);
            response.setBody("User registered successfully");
        } else {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setBody("Registration failed");
        }

        return response;
    }

    private HttpResponse handleLogin(HttpRequest request, HttpResponse response) {
        // Extract login details from request
        // Call userService.loginUser(...)
        // Set appropriate status and body in response
        // Return response

        // Extract registration details from request (mocked for now)
        String username = "exampleUser";
        String password = "examplePassword";

        // Call userService.registerUser() (mocked response)
        boolean isRegistered = userService.registerUser(username, password);

        if (isRegistered) {
            response.setStatus(HttpStatus.OK);
            response.setBody("User registered successfully");
        } else {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setBody("Registration failed");
        }

        return response;
    }

}