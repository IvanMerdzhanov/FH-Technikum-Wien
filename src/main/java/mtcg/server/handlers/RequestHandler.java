package mtcg.server.handlers;

import mtcg.models.*;
import mtcg.server.database.*;
import mtcg.server.http.HttpRequest;
import mtcg.server.http.HttpResponse;
import mtcg.server.http.HttpStatus;
import mtcg.server.models.*;
import mtcg.server.util.JsonSerializer;
import mtcg.server.util.PasswordUtil;
import mtcg.services.IPackageService;
import mtcg.services.IUserService;
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
import java.util.*;
import java.util.stream.Collectors;


public class RequestHandler implements Runnable {
    private final Socket clientSocket;
    private final DatabaseConnector databaseConnector; // Added field for DatabaseConnector
    private final IPackageService packageService;
    private final IUserService userService;


    // Modified constructor to accept DatabaseConnector and IUserService as parameters
    public RequestHandler(Socket clientSocket, DatabaseConnector databaseConnector, IUserService userService, IPackageService packageService) {
        this.clientSocket = clientSocket;
        this.databaseConnector = databaseConnector;
        this.packageService = packageService != null ? packageService : new PackageService(databaseConnector);
        this.userService = userService != null ? userService : UserService.getInstance();
    }


    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream())) {

            // Parse the request
            HttpRequest request = new HttpRequest(in);

            // Log the received URI and method for debugging
            System.out.println("Received request: Method = " + request.getMethod() + ", URI = " + request.getUri());

            // Handle the request based on the URI
            HttpResponse response = handleRequestBasedOnUri(request);

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
            case "/showmycards":
                response = handleShowMyCardsRequest(request);
                break;
            case "/selectCard":
                response = handleSelectCardRequest(request);
                break;
            case "/createTradeOffer":
                response = handleCreateTradeOfferRequest(request);
                break;
            case "/checkOffers":
                response = handleCheckOffersRequest(request);
                break;
            case "/declineOffer":
                response = handleDeclineOfferRequest(request);
                break;
              case "/acceptOffer":
                response = handleAcceptTradeOfferRequest(request);
                break;
            case "/finalizeTrade":
                response = handleTradeCardRequest(request);
                break;
            case "/editProfile":
                response = handleEditProfileRequest(request);
                break;
            case "/changePassword":
                response = checkAuthentication(request) ? handleChangePasswordRequest(request) : unauthorizedResponse();
                break;
            case "/changeUsername":
                response = checkAuthentication(request) ? handleChangeUsernameRequest(request) : unauthorizedResponse();
                break;
            case "/showRecord":
                response = handleShowMyRecordRequest(request);
                break;
            case "/scoreboard":
                response = handleShowScoreboardRequest(request);
                break;
            case "/wallet":
                response = handleWalletRequest(request);
                break;
            default:
                response.setStatus(HttpStatus.NOT_FOUND);
                response.setBody("Endpoint not found");
                break;
        }

        return response;
    }

    private HttpResponse handleEndGameRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();

        // Clear all users and active sessions
        userService.clearAllUsersAndSessions();

        // Reset all cards in the database to taken = false
        resetAllCardsInDatabase();

        response.setStatus(HttpStatus.OK);
        response.setBody("Game ended successfully. All users and sessions cleared, and cards reset.");
        return response;
    }

    private void resetAllCardsInDatabase() {
        String updateQuery = "UPDATE cards SET taken = false";
        try (Connection conn = databaseConnector.connect();
             PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
            updateStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle exceptions, maybe log them or throw a custom exception
        }
    }

    public HttpResponse handleRegisterRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        try {
            System.out.println("Entering handleRegisterRequest");
            RegistrationRequest regRequest = JsonSerializer.deserialize(request.getBody(), RegistrationRequest.class);

            if (regRequest == null) {
                System.out.println("Deserialization resulted in null RegistrationRequest object");
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid registration data");
                return response;
            } else if (regRequest.getUsername() == null || regRequest.getPassword() == null) {
                System.out.println("Missing username or password in registration request");
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid registration data");
                return response;
            }

            String username = regRequest.getUsername();
            String password = regRequest.getPassword();
            System.out.println("Registration attempt for username: " + username);

            try (Connection conn = databaseConnector.connect()) {
                PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM \"users\" WHERE username = ?");
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    System.out.println("Username already taken: " + username);
                    response.setStatus(HttpStatus.BAD_REQUEST);
                    response.setBody("Username already taken");
                    return response;
                }

                String hashedPassword = PasswordUtil.hashPassword(password);
                System.out.println("Password hashed, proceeding with registration");

                PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO \"users\" (username, password) VALUES (?, ?)");
                insertStmt.setString(1, username);
                insertStmt.setString(2, hashedPassword);
                insertStmt.executeUpdate();

                PreparedStatement statsStmt = conn.prepareStatement("INSERT INTO user_statistics (username, total_wins, total_losses, total_draws, elo_rating) VALUES (?, 0, 0, 0, 100)");
                statsStmt.setString(1, username);
                statsStmt.executeUpdate();

                System.out.println("User registered successfully: " + username);
                response.setStatus(HttpStatus.OK);
                response.setBody("User registered successfully");
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in handleRegisterRequest: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Registration failed due to server error");
        }
        return response;
    }



    public HttpResponse handleLoginRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        try {
            System.out.println("Deserializing login request");
            LoginRequest loginRequest = JsonSerializer.deserialize(request.getBody(), LoginRequest.class);

            if (loginRequest == null) {
                System.out.println("Login request deserialization failed");
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid login data");
                return response;
            }

            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();
            System.out.println("Login request received for username: " + username);

            // Check if userService is not null and if the user is already logged in
            System.out.println("Checking if user " + username + " is already logged in");
            if (userService != null && userService.isActiveSession(username)) {
                System.out.println("User " + username + " is already logged in");
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("User already logged in");
                return response;
            }

            System.out.println("Connecting to database");
            try (Connection conn = databaseConnector.connect()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT password FROM \"users\" WHERE username = ?");
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    System.out.println("User found in database. Verifying password for " + username);
                    if (PasswordUtil.checkPassword(password, storedPassword)) {
                        System.out.println("Password check passed for " + username);
                        String token = UUID.randomUUID().toString();
                        System.out.println("Generated token: " + token);

                        PreparedStatement statsStmt = conn.prepareStatement("SELECT total_wins, total_losses, total_draws, elo_rating FROM user_statistics WHERE username = ?");
                        statsStmt.setString(1, username);
                        ResultSet statsRs = statsStmt.executeQuery();

                        if (statsRs.next()) {
                            UserStats stats = new UserStats(
                                    statsRs.getInt("total_wins"),
                                    statsRs.getInt("total_losses"),
                                    statsRs.getInt("total_draws"),
                                    statsRs.getInt("elo_rating")
                            );
                            System.out.println("UserStats retrieved for " + username);

                            User user = new User(username, storedPassword);
                            user.setToken(token);
                            user.setUserStats(stats);

                            if (userService != null) {
                                userService.updateUser(user);
                                userService.addSession(token, username);
                            }
                            System.out.println("User " + username + " updated and session added");

                            response.setBody("Login successful. Token: " + token);
                        } else {
                            System.out.println("UserStats not found for " + username);
                        }
                    } else {
                        System.out.println("Password check failed for " + username);
                        response.setStatus(HttpStatus.BAD_REQUEST);
                        response.setBody("Invalid credentials");
                    }
                } else {
                    System.out.println("User " + username + " not found in database");
                    response.setStatus(HttpStatus.BAD_REQUEST);
                    response.setBody("User not found");
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in handleLoginRequest: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Login failed due to server error");
        }
        return response;
    }

    private HttpResponse handleBattleRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();

        System.out.println("Entering handleBattleRequest");

        if (!isGameReady()) {
            System.out.println("Game is not ready: Not enough players");
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setBody("Not enough players for battle");
            return response;
        }
        try {
            System.out.println("Handling battle request...");

            // Deserialize the request body
            BattleRequestData battleData = JsonSerializer.deserialize(request.getBody(), BattleRequestData.class);
            if (battleData == null) {
                System.out.println("Failed to deserialize battle data");
                throw new RuntimeException("Failed to deserialize battle data");
            }

            // Extract and log tokens
            String tokenPlayerOne = battleData.getTokenPlayerOne();
            String tokenPlayerTwo = battleData.getTokenPlayerTwo();
            System.out.println("Tokens received: Player One - " + tokenPlayerOne + ", Player Two - " + tokenPlayerTwo);

            // Validate tokens
            if (!userService.isActiveSession(tokenPlayerOne) || !userService.isActiveSession(tokenPlayerTwo)) {
                System.out.println("One or both tokens are invalid or missing");
                response.setStatus(HttpStatus.UNAUTHORIZED);
                response.setBody("One or both tokens are invalid or missing");
                return response;
            }

            // Retrieve users and log their details
            String usernamePlayerOne = userService.getUsernameForToken(tokenPlayerOne);
            String usernamePlayerTwo = userService.getUsernameForToken(tokenPlayerTwo);
            System.out.println("Users participating: Player One - " + usernamePlayerOne + ", Player Two - " + usernamePlayerTwo);

            User playerOne = userService.getUser(usernamePlayerOne);
            User playerTwo = userService.getUser(usernamePlayerTwo);

            if (playerOne == null || playerTwo == null) {
                System.out.println("One or both users are null");
                // Handle the case where one or both users are null
            }

            clearTradeOffers(playerOne);
            clearTradeOffers(playerTwo);
            System.out.println("All trade offers cleared for both players.");

            // Log decks
            System.out.println("Player One's Deck: " + playerOne.getDeck().getCards());
            System.out.println("Player Two's Deck: " + playerTwo.getDeck().getCards());

            // Initiate battle
            Battle battle = new Battle(playerOne, playerTwo);
            String winner = battle.startBattle(); // This should now return the winner's username
            System.out.println("Battle started between " + usernamePlayerOne + " and " + usernamePlayerTwo);

            // Update statistics in the database after the battle
            try (Connection conn = databaseConnector.connect()) {
                battle.finishBattle(winner, conn); // Pass the winner and connection to update stats
            } catch (SQLException e) {
                System.err.println("Database error while updating stats: " + e.getMessage());
            }

            // Compile and send response
            BattleResponseData responseData = compileBattleResults(battle, winner); // Pass the winner to the response compilation
            response.setBody(Objects.requireNonNull(JsonSerializer.serialize(responseData)));
            response.setStatus(HttpStatus.OK);
            System.out.println("Battle results compiled...");

        } catch (Exception e) {
            System.err.println("Error in handleBattleRequest: " + e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Error processing battle request");
        }
        System.out.println("Exiting handleBattleRequest");
        return response;
    }

    private boolean isGameReady() {
        System.out.println("Checking if game is ready");
        int activeSessionCount = userService.getActiveSessionsCount();
        System.out.println("Active session count: " + activeSessionCount);

        if (activeSessionCount >= 2) {
            System.out.println("Game is ready with sufficient players.");
            return true;
        } else {
            System.out.println("Game is not ready: Not enough players. Active sessions: " + activeSessionCount);
            return false;
        }
    }


    private void clearTradeOffers(User user) {
        if (user != null && user.getOffers() != null) {
            user.getOffers().clear();
            System.out.println("Trade offers cleared for user: " + user.getUsername());
        }
    }
    private BattleResponseData compileBattleResults(Battle battle, String winner) {
        BattleResponseData responseData = new BattleResponseData();
        responseData.setWinner(winner); // Use the winner from the battle directly
        responseData.setRoundDetails(battle.getRoundResults());
        return responseData;
    }
    public HttpResponse handleLogoutRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        try {
            System.out.println("Entering handleLogoutRequest");

            String authHeader = request.getHeaders().get("Authorization");
            System.out.println("Auth header: " + authHeader);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.out.println("Invalid or missing token in header");
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid or missing token");
                return response;
            }

            String token = authHeader.substring("Bearer ".length());
            System.out.println("Token extracted: " + token);

            if (!userService.isActiveSession(token)) {
                System.out.println("Token is not active: " + token);
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid or missing token");
                return response;
            }

            String username = userService.getUsernameForToken(token);
            System.out.println("Username for token: " + username);

            User user = userService.getUser(username);
            if (user != null) {
                System.out.println("Releasing user cards for: " + username);
                releaseUserCards(user);
                System.out.println("Removing session for token: " + token);
                userService.removeSession(token);
                System.out.println("Removing user: " + username);
                userService.removeUser(user);
            } else {
                System.out.println("No user found for username: " + username);
            }

            response.setStatus(HttpStatus.OK);
            response.setBody("Logout successful");
            System.out.println("Logout successful for: " + username);
        } catch (Exception e) {
            System.err.println("Exception in handleLogoutRequest: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Logout failed due to server error");
        }
        return response;
    }

    private void releaseUserCards(User user) {
        System.out.println("Releasing cards for user: " + user.getUsername());
        // Logic to set user's cards as not taken
        for (Card card : user.getStack()) {
            System.out.println("Setting card as not taken: " + card.getId());
            packageService.setCardAsNotTaken(card.getId());
        }
    }


    private HttpResponse handleGetPackageRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        String authHeader = request.getHeaders().get("Authorization");

        System.out.println("Received handleGetPackageRequest");
        System.out.println("Authorization Header: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("Authorization header is invalid or missing");
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        // Extract the token from the header
        String token = authHeader.substring("Bearer ".length());
        System.out.println("Extracted Token: " + token);

        if (!userService.isActiveSession(token)) {
            System.out.println("Token invalid or missing for token: " + token);
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String username = userService.getUsernameForToken(token);
        System.out.println("Username obtained for token: " + username);

        User user = userService.getUser(username);

        if (user == null) {
            System.out.println("User not found for username: " + username);
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("User not found");
            return response;
        }

        System.out.println("User " + username + " current coins: " + user.getCoins());
        if (user.getCoins() < 5) {
            System.out.println("Insufficient coins for user: " + username);
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setBody("Insufficient coins");
            return response;
        }

        System.out.println("Processing coin spend for user: " + username);
        user.spendCoins();

        List<Card> packageCards = packageService.getPackageCards();
        System.out.println("Number of cards in package: " + packageCards.size());

        user.getStack().addAll(packageCards);
        userService.updateUser(user);

        System.out.println("Package acquired successfully for user: " + username);
        response.setStatus(HttpStatus.OK);
        response.setBody("Package acquired successfully");
        return response;
    }

    private HttpResponse handleShowMyCardsRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        String authHeader = request.getHeaders().get("Authorization");

        System.out.println("Received handleShowMyCardsRequest");
        System.out.println("Authorization Header: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("Authorization header is invalid or missing");
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String token = authHeader.substring("Bearer ".length());
        System.out.println("Extracted Token: " + token);

        if (!userService.isActiveSession(token)) {
            System.out.println("Token is invalid or session is inactive for token: " + token);
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String username = userService.getUsernameForToken(token);
        System.out.println("Username obtained for token: " + username);

        User user = userService.getUser(username);
        if (user == null) {
            System.out.println("User not found for username: " + username);
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("User not found");
            return response;
        }

        System.out.println("Preparing card list for user: " + username);
        StringBuilder cardList = new StringBuilder();
        Map<Card, Integer> cardIndexes = new HashMap<>();
        int index = 1;

        cardList.append("Stack:\n");
        if (user.getStack().isEmpty()) {
            System.out.println("User stack is empty for user: " + username);
            cardList.append("Your stack is empty.\n");
        } else {
            for (Card card : user.getStack()) {
                cardIndexes.put(card, index);
                String inDeck = user.getDeck().getCards().contains(card) ? " (In Deck)" : "";
                cardList.append(index++).append(". ").append(card.getName()).append(" - Damage: ").append(card.getDamage()).append(inDeck).append("\n");
            }
        }

        cardList.append("\nDeck:\n");
        if (user.getDeck().getCards().isEmpty()) {
            System.out.println("User deck is empty for user: " + username);
            cardList.append("Your deck is empty.\n");
        } else {
            for (Card card : user.getDeck().getCards()) {
                Integer deckIndex = cardIndexes.get(card);
                cardList.append(deckIndex != null ? deckIndex : "?").append(". ").append(card.getName()).append(" - Damage: ").append(card.getDamage()).append("\n");
            }
        }

        System.out.println("Sending card list response for user: " + username);
        response.setStatus(HttpStatus.OK);
        response.setBody(cardList.toString());
        return response;
    }


    private HttpResponse handleSelectCardRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        String authHeader = request.getHeaders().get("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String token = authHeader.substring("Bearer ".length());
        if (!userService.isActiveSession(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String username = userService.getUsernameForToken(token);
        User user = userService.getUser(username);
        if (user == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("User not found");
            return response;
        }

        try {
            List<Integer> cardIndexes = parseCardIndexes(request.getBody());
            List<Integer> adjustedIndexes = cardIndexes.stream().map(i -> i - 1).collect(Collectors.toList());

            Deck deck = user.getDeck();
            deck.clear(); // Clear the existing deck

            List<Trading> offers = user.getOffers() != null ? user.getOffers() : new ArrayList<>();
            System.out.println("User's current offers: " + offers);

            for (int index : adjustedIndexes) {
                if (index < 0 || index >= user.getStack().size()) {
                    System.out.println("Invalid card index: " + index);
                    throw new IllegalArgumentException("Invalid card index: " + (index + 1));
                }
                Card selectedCard = user.getStack().get(index);
                System.out.println("Checking card at index " + index + ": " + selectedCard);

                // Check if the card is in the market
                if (TradeMarket.isCardInMarket(selectedCard)) {
                    System.out.println("Card at index " + index + " is part of a trade offer and cannot be added to the deck.");
                    throw new IllegalArgumentException("Card at index " + (index + 1) + " is part of a trade offer and cannot be added to the deck.");
                }

                deck.addCard(selectedCard);
            }

            System.out.println("Cards added to deck successfully.");
            response.setStatus(HttpStatus.OK);
            response.setBody("Cards selected successfully");
        } catch (Exception e) {
            System.out.println("Error in handleSelectCardRequest: " + e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setBody("Invalid card selection: " + e.getMessage());
        }
        return response;
    }
    public List<Integer> parseCardIndexes(String input) {
        List<Integer> indexes = new ArrayList<>();
        try {
            // Manually extract the numbers from the JSON string
            String numbers = input.replaceAll("[^0-9,]", ""); // Remove all but numbers and commas
            String[] parts = numbers.split(",");
            for (String part : parts) {
                indexes.add(Integer.parseInt(part.trim()));
            }
        } catch (Exception e) {
            System.out.println("Error parsing card indexes: " + e.getMessage());
        }
        return indexes;
    }

    private HttpResponse handleCreateTradeOfferRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        String authHeader = request.getHeaders().get("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String token = authHeader.substring("Bearer ".length());
        if (!userService.isActiveSession(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        try {
            TradeOfferRequest tradeOfferRequest = JsonSerializer.deserialize(request.getBody(), TradeOfferRequest.class);
            if (tradeOfferRequest == null) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid trade offer data");
                return response;
            }

            String offeringUsername = userService.getUsernameForToken(token);
            User offeringUser = userService.getUser(offeringUsername);
            User receivingUser = userService.getUser(tradeOfferRequest.getReceivingUsername());
            if (receivingUser == null) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Receiving user not found");
                return response;
            }

            if (receivingUser.getOffers() == null) {
                receivingUser.setOffers(new ArrayList<>());
            }

            Trading newTradeOffer = null;
            switch (tradeOfferRequest.getTypeOfOffer()) {
                case "card-for-card":
                    Card offeredCard = validateCardOffer(offeringUser, tradeOfferRequest.getOfferedCardIndex() - 1);
                    if (offeredCard == null) {
                        response.setStatus(HttpStatus.BAD_REQUEST);
                        response.setBody("Invalid card for trade or card is in deck");
                        return response;
                    }
                    newTradeOffer = new Trading(offeringUser, receivingUser, offeredCard, 0, tradeOfferRequest.getRequestedType(), tradeOfferRequest.getMinimumDamage(), "card-for-card");
                    TradeMarket.addCardToMarket(offeredCard);
                    break;
                case "coins-for-card":
                    if (!validateCoinOffer(offeringUser, tradeOfferRequest.getOfferedCoins())) {
                        response.setStatus(HttpStatus.BAD_REQUEST);
                        response.setBody("Invalid coin amount or insufficient coins");
                        return response;
                    }
                    newTradeOffer = new Trading(offeringUser, receivingUser, null, tradeOfferRequest.getOfferedCoins(), tradeOfferRequest.getRequestedType(), tradeOfferRequest.getMinimumDamage(), "coins-for-card");
                    break;
                case "card-for-coins":
                    offeredCard = validateCardOffer(offeringUser, tradeOfferRequest.getOfferedCardIndex());
                    if (offeredCard == null) {
                        response.setStatus(HttpStatus.BAD_REQUEST);
                        response.setBody("Invalid card for trade or card is in deck");
                        return response;
                    }
                    newTradeOffer = new Trading(offeringUser, receivingUser, offeredCard, tradeOfferRequest.getRequestedCoins(), "Any", 0, "card-for-coins");
                    TradeMarket.addCardToMarket(offeredCard);
                    break;
                default:
                    response.setStatus(HttpStatus.BAD_REQUEST);
                    response.setBody("Invalid trade offer type");
                    return response;
            }

            if (newTradeOffer != null) {
                receivingUser.getOffers().add(newTradeOffer);
                response.setStatus(HttpStatus.OK);
                response.setBody("Trade offer created successfully: " + newTradeOffer.getTradeDetails());
            } else {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Error creating trade offer");
            }
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Error processing trade offer request");
            return response;
        }
    }


    private Card validateCardOffer(User offeringUser, int offeredCardIndex) {
        if (offeredCardIndex <= 0 || offeredCardIndex > offeringUser.getStack().size()) {
            return null; // Invalid card index
        }

        Card offeredCard = offeringUser.getStack().get(offeredCardIndex - 1);
        if (offeringUser.getDeck().getCards().contains(offeredCard)) {
            return null; // Card is in the deck, cannot be offered
        }

        return offeredCard; // Valid card
    }

    private boolean validateCoinOffer(User offeringUser, int offeredCoins) {
        return offeredCoins > 0 && offeringUser.getCoins() >= offeredCoins;
    }

    private HttpResponse handleCheckOffersRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        String authHeader = request.getHeaders().get("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String token = authHeader.substring("Bearer ".length());
        if (!userService.isActiveSession(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String username = userService.getUsernameForToken(token);
        User user = userService.getUser(username);
        if (user == null) {
            System.out.println("Check Offers: User not found for username: " + username);
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("User not found");
            return response;
        }

        System.out.println("Check Offers: Retrieved user: " + username);
        List<Trading> offers = user.getOffers();
        if (offers == null || offers.isEmpty()) {
            System.out.println("Check Offers: No offers available for user: " + username);
            response.setStatus(HttpStatus.OK);
            response.setBody("No trade offers available");
            return response;
        } else {
            System.out.println("Check Offers: Number of offers for user " + username + ": " + offers.size());
        }

        StringBuilder offerDetails = new StringBuilder("Trade offers:\n");
        int index = 1;

        for (Trading offer : offers) {
            offerDetails.append("Offer ").append(index++).append(": ");
            offerDetails.append(offer.getTradeDetails()).append("\n");
        }

        response.setStatus(HttpStatus.OK);
        response.setBody(offerDetails.toString());
        return response;
    }

    private HttpResponse handleDeclineOfferRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        String authHeader = request.getHeaders().get("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String token = authHeader.substring("Bearer ".length());
        if (!userService.isActiveSession(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        try {
            String username = userService.getUsernameForToken(token);
            User user = userService.getUser(username);
            if (user == null) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("User not found");
                return response;
            }

            TradeOfferIndexRequest declineRequest = JsonSerializer.deserialize(request.getBody(), TradeOfferIndexRequest.class);
            if (declineRequest == null) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid request format");
                return response;
            }

            int offerIndex = declineRequest.getOfferIndex() - 1;

            if (offerIndex < 0 || offerIndex >= user.getOffers().size()) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid offer index");
                return response;
            }

            Trading declinedOffer = user.getOffers().remove(offerIndex);
            if (declinedOffer.getOfferedCard() != null) {
                TradeMarket.removeFromMarket(declinedOffer.getOfferedCard());
            }
            response.setStatus(HttpStatus.OK);
            response.setBody("Trade offer declined successfully");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Error processing decline offer request");
        }

        return response;
    }
    private HttpResponse handleAcceptTradeOfferRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        String authHeader = request.getHeaders().get("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String token = authHeader.substring("Bearer ".length());
        if (!userService.isActiveSession(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        try {
            String username = userService.getUsernameForToken(token);
            User user = userService.getUser(username);
            if (user == null) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("User not found");
                return response;
            }

            TradeOfferIndexRequest acceptRequest = JsonSerializer.deserialize(request.getBody(), TradeOfferIndexRequest.class);
            if (acceptRequest == null) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid request format");
                return response;
            }

            int offerIndex = acceptRequest.getOfferIndex() - 1;
            if (offerIndex < 0 || offerIndex >= user.getOffers().size()) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid offer index");
                return response;
            }

            Trading offer = user.getOffers().get(offerIndex);

            switch (offer.getTypeOfOffer()) {
                case "card-for-card":
                    List<Card> eligibleCards = getEligibleCardsForTrade(user, offer.getRequestedType(), offer.getMinimumDamage());
                    String eligibleCardsDisplay = displayEligibleCards(eligibleCards);
                    response.setStatus(HttpStatus.OK);
                    response.setBody(eligibleCardsDisplay);
                    // Remove the offered card from the market upon acceptance
                    if (offer.getOfferedCard() != null) {
                        TradeMarket.removeFromMarket(offer.getOfferedCard());
                    }
                    offer.setState("ACCEPTED");
                    break;
                case "coins-for-card":
                    List<Card> eligibleCardsForCoins = getEligibleCardsForTrade(user, offer.getRequestedType(), offer.getMinimumDamage());
                    String eligibleCardsDisplayForCoins = displayEligibleCards(eligibleCardsForCoins);
                    response.setStatus(HttpStatus.OK);
                    response.setBody(eligibleCardsDisplayForCoins);
                    offer.setState("ACCEPTED");
                    break;
                case "card-for-coins":
                    Card offeredCard = offer.getOfferedCard();
                    int offeredCoins = offer.getOfferedCoins();
                    if (user.getCoins() >= offeredCoins) {
                        // Transfer card from offering user to receiving user
                        transferCardToStack(offer.getOfferingUser(), user, offeredCard);
                        // Transfer coins from receiving user to offering user
                        transferCoins(user, offer.getOfferingUser(), offeredCoins);
                        response.setStatus(HttpStatus.OK);
                        response.setBody("Card and coins trade completed successfully");
                        // Remove the card from the TradeMarket
                        TradeMarket.removeFromMarket(offeredCard);
                        offer.setState("FINALIZED");
                    } else {
                        response.setStatus(HttpStatus.BAD_REQUEST);
                        response.setBody("Insufficient coins for the trade");
                    }
                    break;
                default:
                    response.setStatus(HttpStatus.BAD_REQUEST);
                    response.setBody("Invalid trade offer type");
            }
            return response;
        } catch (Exception e) {
            // Error handling...
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Error processing accept trade offer request");
            return response;
        }
    }

    public String displayEligibleCards(List<Card> eligibleCards) {
        if (eligibleCards.isEmpty()) {
            return "No eligible cards available for trade.";
        }

        StringBuilder displayMessage = new StringBuilder("Eligible Cards for Trade:\n");
        int index = 1;
        for (Card card : eligibleCards) {
            displayMessage.append(index).append(". ")
                    .append(card.getName())
                    .append(" - Type: ").append(card instanceof MonsterCard ? "Monster" : "Spell")
                    .append(", Damage: ").append(card.getDamage()).append("\n");
            index++;
        }

        return displayMessage.toString();
    }

    private HttpResponse handleTradeCardRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        String authHeader = request.getHeaders().get("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String token = authHeader.substring("Bearer ".length());
        if (!userService.isActiveSession(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        try {
            String username = userService.getUsernameForToken(token);
            User user = userService.getUser(username);
            if (user == null) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("User not found");
                return response;
            }

            TradeCardIndexRequest tradeCardRequest = JsonSerializer.deserialize(request.getBody(), TradeCardIndexRequest.class);

            System.out.println("Received tradeOfferIndex: " + tradeCardRequest.getTradeOfferIndex());
            System.out.println("Received cardChoiceIndex: " + tradeCardRequest.getCardChoiceIndex());

            if (tradeCardRequest == null) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid request format");
                return response;
            }

            if (!executeTradeBasedOnCardChoice(user, tradeCardRequest.getTradeOfferIndex(), tradeCardRequest.getCardChoiceIndex())) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid trade card index or trade offer index");
                return response;
            }

            int tradeOfferIndex = tradeCardRequest.getTradeOfferIndex() - 1;
            Trading offer = user.getOffers().get(tradeOfferIndex);
            if (!"ACCEPTED".equals(offer.getState()) || "card-for-coins".equals(offer.getTypeOfOffer())) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid operation or offer not in the correct state");
                return response;
            }
            response.setStatus(HttpStatus.OK);
            response.setBody("Trade executed successfully");
            return response;
        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Error processing trade card request");
            return response;
        }
    }
    private boolean executeTradeBasedOnCardChoice(User user, int tradeOfferIndex, int cardChoiceIndex) {
        System.out.println("Executing trade for user: " + user.getUsername() + ", Offer Index: " + tradeOfferIndex + ", Card Choice Index: " + cardChoiceIndex);

        System.out.println("User's total offers: " + user.getOffers().size());
        System.out.println("Trade offer index being processed: " + tradeOfferIndex);

        if (tradeOfferIndex - 1 < 0 || tradeOfferIndex - 1 >= user.getOffers().size()) {
            System.out.println("Trade offer index is out of range.");
            return false;
        }

        Trading offer = user.getOffers().get(tradeOfferIndex - 1);
        String offerType = offer.getTypeOfOffer();
        User receivingUser = offer.getReceivingUser();

        System.out.println("Offer Type: " + offerType);

        List<Card> eligibleCards = getEligibleCardsForTrade(user, offer.getRequestedType(), offer.getMinimumDamage());
        // Handling card-for-card trades
        if ("card-for-card".equals(offerType)) {
            System.out.println("Processing card-for-card trade.");
            if (cardChoiceIndex <= 0 || cardChoiceIndex > eligibleCards.size()) {
                System.out.println("Invalid card choice index.");
                return false;
            }
            Card chosenCard = eligibleCards.get(cardChoiceIndex - 1);
            System.out.println("Chosen Card: " + chosenCard.getName() + ", Damage: " + chosenCard.getDamage());
            transferCardToStack(receivingUser, offer.getOfferingUser(), chosenCard);

            // Remove chosen card from receiver's deck if present
            if (receivingUser.getDeck().getCards().contains(chosenCard)) {
                receivingUser.getDeck().removeCard(chosenCard);
                System.out.println("Card removed from receiver's deck: " + chosenCard.getName());
            }
            // Remove the card from the TradeMarket
            TradeMarket.removeFromMarket(chosenCard);
        }
        // Handling coins-for-card trades
        if ("coins-for-card".equals(offer.getTypeOfOffer())) {

            System.out.println("Total eligible cards: " + eligibleCards.size());
            System.out.println("Card choice index being processed: " + cardChoiceIndex);

            if (cardChoiceIndex <= 0 || cardChoiceIndex > eligibleCards.size()) {
                System.out.println("Invalid card choice index.");
                return false;
            }
            Card chosenCard = eligibleCards.get(cardChoiceIndex - 1);

            transferCoins(offer.getOfferingUser(), receivingUser, offer.getOfferedCoins());
            transferCardToStack(receivingUser, offer.getOfferingUser(), chosenCard);
            if (receivingUser.getDeck().getCards().contains(chosenCard)) {
                receivingUser.getDeck().removeCard(chosenCard);
                System.out.println("Card removed from receiver's deck: " + chosenCard.getName());
            }
            TradeMarket.removeFromMarket(chosenCard);
        }

        // Transfer the offered card to the receiving user if there is one
        if (offer.getOfferedCard() != null) {
            System.out.println("Transferring offered card.");
            transferCardToStack(offer.getOfferingUser(), receivingUser, offer.getOfferedCard());
            // Remove the card from the TradeMarket
            TradeMarket.removeFromMarket(offer.getOfferedCard());
        }

        // Remove the executed offer from the user's list of offers
        user.getOffers().remove(tradeOfferIndex - 1);

        System.out.println("Trade executed successfully.");
        return true;
    }

    public List<Card> getEligibleCardsForTrade(User user, String requestedType, int minimumDamage) {
        List<Card> userStack = user.getStack();

        // Log user's stack
        System.out.println("User's stack: " + userStack);

        // Filter the stack based on the requested type and minimum damage
        List<Card> eligibleCards = userStack.stream()
                .filter(card -> isCardEligibleForTrade(card, requestedType, minimumDamage))
                .collect(Collectors.toList());

        // Log the filtered eligible cards
        System.out.println("Eligible cards for trade: " + eligibleCards);
        return eligibleCards;
    }

    private boolean isCardEligibleForTrade(Card card, String requestedType, int minimumDamage) {
        // Check if the card is already in the market
        if (TradeMarket.isCardInMarket(card)) {
            System.out.println("Card is in the market and not eligible");
            return false;
        }

        // Check for damage threshold
        if (card.getDamage() < minimumDamage) {
            System.out.println("Card damage too low");
            return false;
        }

        // If the requested type is "Any", return true as only damage matters
        if ("Any".equals(requestedType)) {
            System.out.println("Type is Any, card eligible");
            return true;
        }

        // Check the instance of the card based on the requested type
        if ("Monster".equals(requestedType) && card instanceof MonsterCard) {
            System.out.println("Card is a Monster and eligible");
            return true;
        } else if ("Spell".equals(requestedType) && card instanceof SpellCard) {
            System.out.println("Card is a Spell and eligible");
            return true;
        }

        System.out.println("Card does not match the requested type");
        return false; // Card does not match the requested type
    }
    private void transferCoins(User fromUser, User toUser, int amount) {
        if (fromUser.getCoins() >= amount) {
            fromUser.setCoins(fromUser.getCoins() - amount);
            toUser.setCoins(toUser.getCoins() + amount);
        } else {
            System.out.println("Insufficient coins for the transfer.");
        }
    }

    private void transferCardToStack(User fromUser, User toUser, Card card) {
        System.out.println("Transferring card from " + fromUser.getUsername() + " to " + toUser.getUsername());
        System.out.println("Card to transfer: " + card.getName() + ", Damage: " + card.getDamage());

        // Remove card from the offering user's stack
        boolean removed = fromUser.getStack().remove(card);
        System.out.println("Card removed from offering user's stack: " + removed);

        // Add card to the receiving user's stack
        toUser.getStack().add(card);
        System.out.println("Card added to receiving user's stack.");

        // Optionally, print out the current stacks for both users for verification
        System.out.println("Offering user's current stack: " + fromUser.getStack());
        System.out.println("Receiving user's current stack: " + toUser.getStack());
    }

    private boolean isAuthenticated(HttpRequest request) {
        String authHeader = request.getHeaders().get("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false; // Token not provided or invalid format
        }

        String token = authHeader.substring("Bearer ".length());
        if (!userService.isActiveSession(token)) {
            return false; // Session not active or token invalid
        }

        // Retrieve the username associated with the token
        String username = userService.getUsernameForToken(token);

        // Check if a user is associated with the username
        return userService.getUser(username) != null;
    }


    private HttpResponse unauthorizedResponse() {
        HttpResponse response = new HttpResponse();
        response.setStatus(HttpStatus.UNAUTHORIZED);
        response.setBody("User not authenticated");
        return response;
    }
    private boolean checkAuthentication(HttpRequest request) {
        String authHeader = request.getHeaders().get("Authorization");

        // Check if the authorization header is present and correctly formatted
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }

        // Extract the token from the header
        String token = authHeader.substring("Bearer ".length());

        // Check if the token is active (i.e., the session is active)
        if (!userService.isActiveSession(token)) {
            return false;
        }

        // Retrieve the username associated with the token
        String username = userService.getUsernameForToken(token);

        // Check if a user is associated with the username
        return userService.getUser(username) != null;
    }
    private HttpResponse handleEditProfileRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();

        // Check if the user is authenticated
        boolean isAuthenticated = isAuthenticated(request);
        if (!isAuthenticated) {
            return unauthorizedResponse(); // Return a response indicating unauthorized access
        }

        try {
            // Deserialize the edit profile request
            EditProfileRequest editRequest = JsonSerializer.deserialize(request.getBody(), EditProfileRequest.class);
            if (editRequest == null) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid edit profile data");
                return response;
            }

            String username = userService.getUsernameForToken(request.getHeaders().get("Authorization").substring("Bearer ".length()));
            User user = userService.getUser(username);
            // Perform the profile update operations
            if (editRequest.getNewUsername() != null) {
                // Update username operation
                try (Connection conn = databaseConnector.connect()) {
                    // Check if the new username is already taken
                    PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM \"users\" WHERE username = ?");
                    checkStmt.setString(1, editRequest.getNewUsername());
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next()) {
                        response.setStatus(HttpStatus.BAD_REQUEST);
                        response.setBody("New username already taken");
                        return response;
                    }

                    // Update the username in the database
                    PreparedStatement updateStmt = conn.prepareStatement("UPDATE \"users\" SET username = ? WHERE username = ?");
                    updateStmt.setString(1, editRequest.getNewUsername());
                    updateStmt.setString(2, username); // username from the token
                    updateStmt.executeUpdate();
                }
            }

            if (editRequest.getNewPassword() != null) {
                // Update password operation
                try (Connection conn = databaseConnector.connect()) {
                    String hashedPassword = PasswordUtil.hashPassword(editRequest.getNewPassword());

                    // Update the password in the database
                    PreparedStatement updateStmt = conn.prepareStatement("UPDATE \"users\" SET password = ? WHERE username = ?");
                    updateStmt.setString(1, hashedPassword);
                    updateStmt.setString(2, username); // username from the token
                    updateStmt.executeUpdate();
                }
            }
            // Set success response
            response.setStatus(HttpStatus.OK);
            response.setBody("Profile can be updated");
            user.setCanChangeCredentials(true);
            userService.updateUser(user);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Error processing edit profile request");
        }

        return response;
    }
    private HttpResponse handleChangePasswordRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();

        // Extract the token and check if the user is authenticated
        boolean isAuthenticated = isAuthenticated(request);
        if (!isAuthenticated) {
            return unauthorizedResponse(); // Return a response indicating unauthorized access
        }

        try {
            // Deserialize the change password request
            ChangePasswordRequest changePasswordRequest = JsonSerializer.deserialize(request.getBody(), ChangePasswordRequest.class);
            if (changePasswordRequest == null) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid change password data");
                return response;
            }

            String username = userService.getUsernameForToken(request.getHeaders().get("Authorization").substring("Bearer ".length()));
            User user = userService.getUser(username);
            // Check if the user is allowed to change credentials
            if (!user.canChangeCredentials()) {
                return unauthorizedResponse();
            }
            // Ensure the new password is different from the old password
            String newHashedPassword = PasswordUtil.hashPassword(changePasswordRequest.getNewPassword());

            if (PasswordUtil.checkPassword(changePasswordRequest.getNewPassword(), user.getPassword())) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("New password cannot be the same as the old password");
                return response;
            }

            // Update the password in the database
            try (Connection conn = databaseConnector.connect()) {
                PreparedStatement updateStmt = conn.prepareStatement("UPDATE \"users\" SET password = ? WHERE username = ?");
                updateStmt.setString(1, newHashedPassword);
                updateStmt.setString(2, username);
                updateStmt.executeUpdate();
            }

            // Set success response
            response.setStatus(HttpStatus.OK);
            response.setBody("Password changed successfully");
            user.setCanChangeCredentials(false);
            userService.updateUser(user);
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Error processing change password request");
        }

        return response;
    }
    private HttpResponse handleChangeUsernameRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();

        // Check if the user is authenticated
        if (!isAuthenticated(request)) {
            return unauthorizedResponse();
        }

        try {
            // Deserialize the change username request
            ChangeUsernameRequest changeRequest = JsonSerializer.deserialize(request.getBody(), ChangeUsernameRequest.class);
            if (changeRequest == null || changeRequest.getNewUsername() == null || changeRequest.getNewUsername().isEmpty()) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid change username data");
                return response;
            }

            // Extract username from the token
            String token = request.getHeaders().get("Authorization").substring("Bearer ".length());
            String currentUsername = userService.getUsernameForToken(token);
            User user = userService.getUser(currentUsername);
            // Check if the user is allowed to change credentials
            if (!user.canChangeCredentials()) {
                return unauthorizedResponse();
            }
            // Update the username
            try (Connection conn = databaseConnector.connect()) {
                // Check if the new username is already in use
                PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM \"users\" WHERE username = ?");
                checkStmt.setString(1, changeRequest.getNewUsername());
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    response.setStatus(HttpStatus.BAD_REQUEST);
                    response.setBody("Username already in use");
                    return response;
                }

                // Update username in the database
                PreparedStatement updateStmt = conn.prepareStatement("UPDATE \"users\" SET username = ? WHERE username = ?");
                updateStmt.setString(1, changeRequest.getNewUsername());
                updateStmt.setString(2, currentUsername);
                updateStmt.executeUpdate();

                // Update username in the UserService
                userService.updateUsername(currentUsername, changeRequest.getNewUsername());

                response.setStatus(HttpStatus.OK);
                response.setBody("Username updated successfully");
                user.setCanChangeCredentials(false);
                userService.updateUser(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Error processing change username request");
        }
        return response;
    }
    private HttpResponse handleShowMyRecordRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        String authHeader = request.getHeaders().get("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String token = authHeader.substring("Bearer ".length());
        if (!userService.isActiveSession(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String username = userService.getUsernameForToken(token);
        User user = userService.getUser(username);
        if (user == null || user.getUserStats() == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("User or user statistics not found");
            return response;
        }

        UserStats stats = user.getUserStats();
        StringBuilder record = new StringBuilder();
        record.append("Record for ").append(username).append(":\n");
        record.append("Wins: ").append(stats.getTotalWins()).append("\n");
        record.append("Losses: ").append(stats.getTotalLosses()).append("\n");
        record.append("Draws: ").append(stats.getTotalDraws()).append("\n");
        record.append("ELO Rating: ").append(stats.getEloRating()).append("\n");

        response.setStatus(HttpStatus.OK);
        response.setBody(record.toString());
        return response;
    }
    private HttpResponse handleShowScoreboardRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();

        try (Connection conn = databaseConnector.connect()) {
            // SQL query to fetch and sort user statistics
            String sql = "SELECT username, total_wins, total_losses, total_draws, elo_rating FROM user_statistics ORDER BY elo_rating DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            StringBuilder scoreboard = new StringBuilder();
            scoreboard.append(String.format("%-5s %-20s %-10s %-10s %-10s %-10s\n", "Rank", "Username", "Wins", "Losses", "Draws", "ELO"));
            int rank = 1;
            while (rs.next()) {
                String username = rs.getString("username");
                int totalWins = rs.getInt("total_wins");
                int totalLosses = rs.getInt("total_losses");
                int totalDraws = rs.getInt("total_draws");
                int eloRating = rs.getInt("elo_rating");

                scoreboard.append(String.format("%-5d %-20s %-10d %-10d %-10d %-10d\n", rank++, username, totalWins, totalLosses, totalDraws, eloRating));
            }

            response.setStatus(HttpStatus.OK);
            response.setBody(scoreboard.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Error retrieving scoreboard data");
        }

        return response;
    }
    private HttpResponse handleWalletRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        String authHeader = request.getHeaders().get("Authorization");

        System.out.println("handleWalletRequest: Received request");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("handleWalletRequest: Authorization header is invalid or missing");
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String token = authHeader.substring("Bearer ".length());
        System.out.println("handleWalletRequest: Extracted Token - " + token);

        if (!userService.isActiveSession(token)) {
            System.out.println("handleWalletRequest: Token invalid or not active - " + token);
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String username = userService.getUsernameForToken(token);
        System.out.println("handleWalletRequest: Username obtained for token - " + username);

        User user = userService.getUser(username);
        if (user == null) {
            System.out.println("handleWalletRequest: User not found for username - " + username);
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("User not found");
            return response;
        }

        int userCoins = user.getCoins(); // Retrieve the number of coins the user has
        System.out.println("handleWalletRequest: User " + username + " has coins - " + userCoins);

        response.setStatus(HttpStatus.OK);
        response.setBody("Coins: " + userCoins); // Respond with the number of coins
        System.out.println("handleWalletRequest: Response sent with user coins");

        return response;
    }


}