package mtcg.server.handlers;

import mtcg.models.Battle;
import mtcg.models.ElementType;
import mtcg.models.MonsterCard;
import mtcg.models.User;
import mtcg.server.database.DatabaseConnector;
import mtcg.server.http.*;
import mtcg.server.models.*;
import mtcg.server.util.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.HashSet;
import java.util.Set;


public class RequestHandler implements Runnable {
    private final Socket clientSocket;
    private static final Set<String> activeSessions = new HashSet<>();

    HttpResponse response;
    boolean isGameReady = activeSessions.size() >= 2;

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
            if (isGameReady || "/register".equals(request.getUri()) || "/login".equals(request.getUri()) || "/endgame".equals(request.getUri())) {
                // Handle all endpoints if the game is ready, or only login and register if not
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
            // Add more cases for other endpoints as needed
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
            if (loginRequest == null) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid login data");
                return response;
            }

            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();

            try (Connection conn = DatabaseConnector.connect()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT password FROM \"users\" WHERE username = ?");
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    if (PasswordUtil.checkPassword(password, storedPassword)) {
                        // Generate token or set login success response
                        activeSessions.add(username);
                        response.setStatus(HttpStatus.OK);
                        response.setBody("Login successful");
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


    private BattleResponseData compileBattleResults(Battle battle) {
        BattleResponseData responseData = new BattleResponseData();

        responseData.setWinner(battle.determineWinner());
        responseData.setRoundDetails(battle.getRoundResults());

        return responseData;
    }
}