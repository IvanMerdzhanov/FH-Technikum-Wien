package mtcg.server.handlers;

import mtcg.models.*;
import mtcg.server.database.DatabaseConnector;
import mtcg.server.http.HttpRequest;
import mtcg.server.http.HttpResponse;
import mtcg.server.http.HttpStatus;
import mtcg.server.models.BattleRequestData;
import mtcg.server.models.BattleResponseData;
import mtcg.server.models.LoginRequest;
import mtcg.server.models.RegistrationRequest;
import mtcg.server.util.JsonSerializer;
import mtcg.server.util.PasswordUtil;
import mtcg.services.PackageService;
import mtcg.services.UserService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class RequestHandler implements Runnable {
    private final Socket clientSocket;
    private static Map<String, String> activeSessions = new ConcurrentHashMap<>();

    HttpResponse response;
    boolean isGameReady = UserService.getActiveSessionsCount() >= 2;

    public RequestHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream())) {

            // Parse the request
            HttpRequest request = new HttpRequest(in);

            // Log the received URI and method for debugging
            System.out.println("Received request: Method = " + request.getMethod() + ", URI = " + request.getUri());

            // Determine the response based on the URI and game state
            HttpResponse response;
            if (isGameReady || "/register".equals(request.getUri()) || "/login".equals(request.getUri()) || "/logout".equals(request.getUri()) || "/endgame".equals(request.getUri())){
                response = handleRequestBasedOnUri(request);
            } else {
                // Restrict access if the game is not ready
                response = new HttpResponse();
                response.setStatus(HttpStatus.NOT_FOUND);
                response.setBody("Game not ready or endpoint not found");
            }

            // Send the response
            out.print(response.buildResponse());
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private HttpResponse handleRequestBasedOnUri(HttpRequest request) {
        HttpResponse response = new HttpResponse();

        switch (request.getUri()) {
            case "/register":
                response = handleRegisterRequest(request);
                break;
            case "/login":
                response = handleLoginRequest(request);
                break;
            case "/battle":
                System.out.println("Goint to /battle");
                response = handleBattleRequest(request);
                break;
            case "/endgame":
                response = handleEndGameRequest(request);
                break;
            case "/logout":
                response = handleLogoutRequest(request);
                break;
            case "/getpackage":
                response = handleGetPackageRequest(request);
                break;
            default:
                response.setStatus(HttpStatus.NOT_FOUND);
                response.setBody("Endpoint not found");
                break;
        }

        return response;
    }

    private HttpResponse handleEndGameRequest(HttpRequest request) {
        // Logic to handle endgame request
        HttpResponse response = new HttpResponse();
        // Set appropriate response status and body
        // Example:
        response.setStatus(HttpStatus.OK);
        response.setBody("Game ended successfully");
        // Reset or update game state as needed
        return response;
    }


    public HttpResponse handleRegisterRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        try {
            RegistrationRequest regRequest = JsonSerializer.deserialize(request.getBody(), RegistrationRequest.class);
            if (regRequest == null) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid registration data");
                return response;
            }

            String username = regRequest.getUsername();
            String password = regRequest.getPassword();

            try (Connection conn = DatabaseConnector.connect()) {
                PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM \"users\" WHERE username = ?");
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    response.setStatus(HttpStatus.BAD_REQUEST);
                    response.setBody("Username already taken");
                    return response;
                }

                String hashedPassword = PasswordUtil.hashPassword(password);

                PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO \"users\" (username, password, coins) VALUES (?, ?, ?)");
                insertStmt.setString(1, username);
                insertStmt.setString(2, hashedPassword);
                insertStmt.setInt(3, 20); // Default starting coins
                insertStmt.executeUpdate();

                response.setStatus(HttpStatus.OK);
                response.setBody("User registered successfully");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Registration failed due to server error");
        }
        return response;
    }

    public HttpResponse handleLoginRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        try {
            LoginRequest loginRequest = JsonSerializer.deserialize(request.getBody(), LoginRequest.class);
            System.out.println("Login request received for username: " + loginRequest.getUsername());

            if (loginRequest == null) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid login data");
                return response;
            }

            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();

            // Check if the user is already logged in
            if (activeSessions.containsValue(username)) {
                System.out.println("User " + username + " is already logged in.");
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("User already logged in");
                return response;
            }

            try (Connection conn = DatabaseConnector.connect()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT password FROM \"users\" WHERE username = ?");
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    if (PasswordUtil.checkPassword(password, storedPassword)) {
                        System.out.println("Password check passed for user: " + username);
                        // Generate a unique token
                        String token = UUID.randomUUID().toString();
                        System.out.println("Generated token: " + token + " for user: " + username);
                        User user = new User(username, storedPassword);
                        user.setToken(token);
                        UserService.updateUser(user); // Add user to UserService
                        UserService.addSession(token, username);

                        UserService.addSession(token, username);
                        System.out.println("Current active sessions: " + activeSessions);

                       // response.setStatus(HttpStatus.OK);
                        response.setBody("Login successful. Token: " + token); // Include the token in the response
                    } else {
                        response.setStatus(HttpStatus.BAD_REQUEST);
                        response.setBody("Invalid credentials");
                    }
                } else {
                    response.setStatus(HttpStatus.BAD_REQUEST);
                    response.setBody("User not found");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Login failed due to server error");
        }
        return response;
    }

    private HttpResponse handleBattleRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        try {
            System.out.println("Handling battle request...");

            BattleRequestData battleData = JsonSerializer.deserialize(request.getBody(), BattleRequestData.class);
            if (battleData == null) {
                throw new RuntimeException("Failed to deserialize battle data");
            }

            User playerOne = battleData.getPlayerOne();
            User playerTwo = battleData.getPlayerTwo();
            System.out.println("Players retrieved: " + playerOne.getUsername() + ", " + playerTwo.getUsername());

            // Add cards to players for testing
            playerOne.addCardtoDeck(new MonsterCard("M7", "FireElf", 15, ElementType.FIRE));
            playerTwo.addCardtoDeck(new MonsterCard("M8", "Dragon", 20, ElementType.FIRE));

            Battle battle = new Battle(playerOne, playerTwo);
            battle.startBattle();
            System.out.println("Battle started...");

            BattleResponseData responseData = compileBattleResults(battle);
            System.out.println("Battle results compiled...");

            response.setBody(JsonSerializer.serialize(responseData));
            response.setStatus(HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("Error in handleBattleRequest: " + e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Error processing battle request");
        }
        return response;
    }
    public HttpResponse handleLogoutRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        try {
            String authHeader = request.getHeaders().get("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid or missing token");
                return response;
            }

            String token = authHeader.substring("Bearer ".length());

            // Retrieve username associated with the token
            String username = UserService.getUsernameForToken(token);
            if (username == null) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Session not found");
                return response;
            }

            // Remove the session and the user
            if (username != null) {
                UserService.removeSession(token);
                UserService.deleteUser(UserService.getUser(username));
            }

            response.setStatus(HttpStatus.OK);
            response.setBody("Logout successful");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Logout failed due to server error");
        }
        return response;
    }



    private HttpResponse handleGetPackageRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        String authHeader = request.getHeaders().get("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        // Extract the token from the header
        String token = authHeader.substring("Bearer ".length());

        System.out.println("Received token: " + token);
        System.out.println("Token valid: " + activeSessions.containsKey(token));

        if (!UserService.isActiveSession(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String username = UserService.getUsernameForToken(token);

        // Use UserService to get the User object
        User user = UserService.getUser(username);

        if (user == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("User not found");
            return response;
        }

        if (user.getCoins() < 5) {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setBody("Insufficient coins");
            return response;
        }

        user.spendCoins();
        List<Card> packageCards = PackageService.getPackageCards();
        user.getStack().addAll(packageCards);

        UserService.updateUser(user);

        response.setStatus(HttpStatus.OK);
        response.setBody("Package acquired successfully");
        return response;
    }


    private BattleResponseData compileBattleResults(Battle battle) {
        BattleResponseData responseData = new BattleResponseData();

        responseData.setWinner(battle.determineWinner());
        responseData.setRoundDetails(battle.getRoundResults());

        return responseData;
    }
}