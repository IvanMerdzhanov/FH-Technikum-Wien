package mtcg.server.handlers;

import mtcg.models.*;
import mtcg.server.database.DatabaseConnector;
import mtcg.server.http.HttpRequest;
import mtcg.server.http.HttpResponse;
import mtcg.server.http.HttpStatus;
import mtcg.server.models.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


public class RequestHandler implements Runnable {
    private final Socket clientSocket;
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
            case "/showmycards":
                response = handleShowMyCardsRequest(request);
                break;
            case "/selectCard":
                response = handleSelectCardRequest(request);
                break;
            case "/showDeck":
                response = handleShowDeckRequest(request);
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
            case "/editProfile":
                response = handleEditProfileRequest(request);
                break;
            case "/changePassword":
                response = checkAuthentication(request) ? handleChangePasswordRequest(request) : unauthorizedResponse();
                break;
            case "/changeUsername":
                response = checkAuthentication(request) ? handleChangeUsernameRequest(request) : unauthorizedResponse();
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
        UserService.clearAllUsersAndSessions();

        // Reset all cards in the database to taken = false
        resetAllCardsInDatabase();

        response.setStatus(HttpStatus.OK);
        response.setBody("Game ended successfully. All users and sessions cleared, and cards reset.");
        return response;
    }

    private void resetAllCardsInDatabase() {
        String updateQuery = "UPDATE cards SET taken = false";
        try (Connection conn = DatabaseConnector.connect();
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
            if (UserService.isActiveSession(username)) { // Use UserService to check active session
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

                        System.out.println("Session added for user: " + username + ", Token: " + token);
                        System.out.println("Current active sessions: " + UserService.getActiveSessions());

                        response.setBody("Login successful. Token: " + token);
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

            // Deserialize the request body
            BattleRequestData battleData = JsonSerializer.deserialize(request.getBody(), BattleRequestData.class);
            if (battleData == null) {
                throw new RuntimeException("Failed to deserialize battle data");
            }

            // Extract and log tokens
            String tokenPlayerOne = battleData.getTokenPlayerOne();
            String tokenPlayerTwo = battleData.getTokenPlayerTwo();
            System.out.println("Tokens received: Player One - " + tokenPlayerOne + ", Player Two - " + tokenPlayerTwo);

            // Validate tokens
            if (!UserService.isActiveSession(tokenPlayerOne) || !UserService.isActiveSession(tokenPlayerTwo)) {
                response.setStatus(HttpStatus.UNAUTHORIZED);
                response.setBody("One or both tokens are invalid or missing");
                return response;
            }

            // Retrieve users and log their details
            String usernamePlayerOne = UserService.getUsernameForToken(tokenPlayerOne);
            String usernamePlayerTwo = UserService.getUsernameForToken(tokenPlayerTwo);
            System.out.println("Users participating: Player One - " + usernamePlayerOne + ", Player Two - " + usernamePlayerTwo);

            User playerOne = UserService.getUser(usernamePlayerOne);
            User playerTwo = UserService.getUser(usernamePlayerTwo);

            // Log decks
            System.out.println("Player One's Deck: " + playerOne.getDeck().getCards());
            System.out.println("Player Two's Deck: " + playerTwo.getDeck().getCards());

            // Initiate battle
            Battle battle = new Battle(playerOne, playerTwo);
            battle.startBattle();
            System.out.println("Battle started...");

            // Compile and send response
            BattleResponseData responseData = compileBattleResults(battle);
            response.setBody(JsonSerializer.serialize(responseData));
            response.setStatus(HttpStatus.OK);
            System.out.println("Battle results compiled...");

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
            if (!UserService.isActiveSession(token)) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid or missing token");
                return response;
            }

            String username = UserService.getUsernameForToken(token);
            User user = UserService.getUser(username);
            if (user != null) {
                releaseUserCards(user);
                UserService.removeSession(token);
                UserService.removeUser(user);
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

    private void releaseUserCards(User user) {
        // Logic to set user's cards as not taken
        for (Card card : user.getStack()) {
            // Assuming a method in PackageService to update a card's taken status
            PackageService.setCardAsNotTaken(card.getId());
        }
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

        if (!UserService.isActiveSession(token)) {
            System.out.println("Token invalid or missing for token: " + token);
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String username = UserService.getUsernameForToken(token);

        User user = UserService.getUser(username);

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

        user.spendCoins();

        List<Card> packageCards = PackageService.getPackageCards();

        user.getStack().addAll(packageCards);
        UserService.updateUser(user);

        response.setStatus(HttpStatus.OK);
        response.setBody("Package acquired successfully");
        return response;
    }

    private HttpResponse handleShowMyCardsRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        String authHeader = request.getHeaders().get("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String token = authHeader.substring("Bearer ".length());
        if (!UserService.isActiveSession(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String username = UserService.getUsernameForToken(token);
        User user = UserService.getUser(username);
        if (user == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("User not found");
            return response;
        }

        StringBuilder cardList = new StringBuilder();
        for (Card card : user.getStack()) {
            cardList.append(card.getName()).append("\n");
        }

        response.setStatus(HttpStatus.OK);
        response.setBody("Your cards:\n" + cardList);
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
        if (!UserService.isActiveSession(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String username = UserService.getUsernameForToken(token);
        User user = UserService.getUser(username);
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

            for (int index : adjustedIndexes) {
                if (index < 0 || index >= user.getStack().size()) {
                    throw new IllegalArgumentException("Invalid card index: " + (index + 1));
                }
                deck.addCard(user.getStack().get(index));
            }

            response.setStatus(HttpStatus.OK);
            response.setBody("Cards selected successfully");
        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setBody("Invalid card selection: " + e.getMessage());
            System.out.println("Error in handleSelectCardRequest: " + e.getMessage());
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
    private HttpResponse handleShowDeckRequest(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        String authHeader = request.getHeaders().get("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String token = authHeader.substring("Bearer ".length());
        if (!UserService.isActiveSession(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String username = UserService.getUsernameForToken(token);
        User user = UserService.getUser(username);
        if (user == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("User not found");
            return response;
        }

        StringBuilder cardList = new StringBuilder();
        for (Card card : user.getDeck().getCards()) {
            cardList.append(card.getName()).append("\n");
        }

        response.setStatus(HttpStatus.OK);
        response.setBody("Your deck:\n" + cardList);
        return response;
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
        if (!UserService.isActiveSession(token)) {
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

            String offeringUsername = UserService.getUsernameForToken(token);
            User offeringUser = UserService.getUser(offeringUsername);
            User receivingUser = UserService.getUser(tradeOfferRequest.getReceivingUsername());

            // Process the offered card
            Card offeredCard = null;
            if (tradeOfferRequest.getOfferedCardIndex() != null) {
                int cardIndex = tradeOfferRequest.getOfferedCardIndex() - 1;
                if (cardIndex >= 0 && cardIndex < offeringUser.getStack().size()) {
                    offeredCard = offeringUser.getStack().get(cardIndex);
                }
            }

            // Process the requested card
            Card requestedCard = null;
            if (tradeOfferRequest.getRequestedCardIndex() != null) {
                int cardIndex = tradeOfferRequest.getRequestedCardIndex() - 1;
                if (cardIndex >= 0 && cardIndex < receivingUser.getStack().size()) {
                    requestedCard = receivingUser.getStack().get(cardIndex);
                }
            }

            if ((tradeOfferRequest.getOfferedCardIndex() != null && (tradeOfferRequest.getOfferedCardIndex() <= 0 || tradeOfferRequest.getOfferedCardIndex() > offeringUser.getStack().size())) ||
                    (tradeOfferRequest.getRequestedCardIndex() != null && (tradeOfferRequest.getRequestedCardIndex() <= 0 || tradeOfferRequest.getRequestedCardIndex() > receivingUser.getStack().size()))) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid card index");
                return response;
            }

            Trading tradeOffer = new Trading(offeringUser, receivingUser, offeredCard, tradeOfferRequest.getOfferedCoins(), requestedCard, tradeOfferRequest.getRequestedCoins());

            if (!validateOffer(offeringUser, receivingUser, tradeOffer)) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid offer, check card ownership or coin balance");
                return response;
            }

            // Add the trade offer to the receiving user's offers
            List<Trading> receivingUserOffers = receivingUser.getOffers();
            if (receivingUserOffers == null) {
                receivingUserOffers = new ArrayList<>();
                receivingUser.setOffers(receivingUserOffers);
            }
            receivingUserOffers.add(tradeOffer);

            response.setStatus(HttpStatus.OK);
            response.setBody("Trade offer created successfully");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Error processing trade offer request");
        }

        return response;
    }


    private boolean validateOffer(User offeringUser, User receivingUser, Trading tradeOffer) {
        // Check if offering user has the card and coins they are offering
        if (tradeOffer.getOfferedCard() != null && !offeringUser.getStack().contains(tradeOffer.getOfferedCard())) {
            return false; // Offering user doesn't own the card
        }
        if (tradeOffer.getOfferedCoins() > 0 && offeringUser.getCoins() < tradeOffer.getOfferedCoins()) {
            return false; // Offering user doesn't have enough coins
        }

        // Check if receiving user has the card and coins being requested
        if (tradeOffer.getRequestedCard() != null && !receivingUser.getStack().contains(tradeOffer.getRequestedCard())) {
            return false; // Receiving user doesn't own the requested card
        }
        if (tradeOffer.getRequestedCoins() > 0 && receivingUser.getCoins() < tradeOffer.getRequestedCoins()) {
            return false; // Receiving user doesn't have enough coins
        }

        return true; // Valid offer
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
        if (!UserService.isActiveSession(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        String username = UserService.getUsernameForToken(token);
        User user = UserService.getUser(username);
        if (user == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("User not found");
            return response;
        }

        List<Trading> offers = user.getOffers();
        if (offers == null || offers.isEmpty()) {
            response.setStatus(HttpStatus.OK);
            response.setBody("No trade offers available");
            return response;
        }

        StringBuilder offerDetails = new StringBuilder("Trade offers:\n");
        int index = 1;

        for (Trading offer : offers) {
            offerDetails.append("Offer ").append(index++).append(": ");
            offerDetails.append(offer.getOfferDetails()).append("\n");
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
        if (!UserService.isActiveSession(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        try {
            String username = UserService.getUsernameForToken(token);
            User user = UserService.getUser(username);
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

            user.getOffers().remove(offerIndex);
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
        if (!UserService.isActiveSession(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED);
            response.setBody("Invalid or missing token");
            return response;
        }

        try {
            String username = UserService.getUsernameForToken(token);
            User user = UserService.getUser(username);
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

            int offerIndex = acceptRequest.getOfferIndex() - 1; // Adjust for 0-based indexing

            if (offerIndex < 0 || offerIndex >= user.getOffers().size()) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid offer index");
                return response;
            }

            Trading offer = user.getOffers().get(offerIndex);
            executeTrade(offer);
            user.getOffers().remove(offerIndex);

            response.setStatus(HttpStatus.OK);
            response.setBody("Trade offer accepted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Error processing accept trade offer request");
        }

        return response;
    }

    private void executeTrade(Trading offer) {
        User offeringUser = offer.getOfferingUser();
        User receivingUser = offer.getReceivingUser();

        // Transfer the offered card from offering user to receiving user
        if (offer.getOfferedCard() != null) {
            transferCardToStack(offeringUser, receivingUser, offer.getOfferedCard());
        }

        // Transfer the requested card from receiving user to offering user
        if (offer.getRequestedCard() != null) {
            transferCardToStack(receivingUser, offeringUser, offer.getRequestedCard());
        }

        // Transfer the offered coins from offering user to receiving user
        if (offer.getOfferedCoins() > 0) {
            transferCoins(offeringUser, receivingUser, offer.getOfferedCoins());
        }

        // Transfer the requested coins from receiving user to offering user
        if (offer.getRequestedCoins() > 0) {
            transferCoins(receivingUser, offeringUser, offer.getRequestedCoins());
        }
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
        fromUser.getStack().remove(card); // Remove card from the offering user's stack
        toUser.getStack().add(card);      // Add card to the receiving user's stack
    }
    private boolean isAuthenticated(HttpRequest request) {
        String authHeader = request.getHeaders().get("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false; // Token not provided or invalid format
        }

        String token = authHeader.substring("Bearer ".length());
        if (!UserService.isActiveSession(token)) {
            return false; // Session not active or token invalid
        }

        // Retrieve the username associated with the token
        String username = UserService.getUsernameForToken(token);

        // Check if a user is associated with the username
        return UserService.getUser(username) != null;
    }


    private String extractPasswordFromRequestBody(String requestBody) {
        LoginRequest loginRequest = JsonSerializer.deserialize(requestBody, LoginRequest.class);
        return loginRequest != null ? loginRequest.getPassword() : null;
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
        if (!UserService.isActiveSession(token)) {
            return false;
        }

        // Retrieve the username associated with the token
        String username = UserService.getUsernameForToken(token);

        // Check if a user is associated with the username
        return UserService.getUser(username) != null;
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

            String username = UserService.getUsernameForToken(request.getHeaders().get("Authorization").substring("Bearer ".length()));
            User user = UserService.getUser(username);
            // Perform the profile update operations
            if (editRequest.getNewUsername() != null) {
                // Update username operation
                try (Connection conn = DatabaseConnector.connect()) {
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
                try (Connection conn = DatabaseConnector.connect()) {
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
            UserService.updateUser(user);
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

            String username = UserService.getUsernameForToken(request.getHeaders().get("Authorization").substring("Bearer ".length()));
            User user = UserService.getUser(username);
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
            try (Connection conn = DatabaseConnector.connect()) {
                PreparedStatement updateStmt = conn.prepareStatement("UPDATE \"users\" SET password = ? WHERE username = ?");
                updateStmt.setString(1, newHashedPassword);
                updateStmt.setString(2, username);
                updateStmt.executeUpdate();
            }

            // Set success response
            response.setStatus(HttpStatus.OK);
            response.setBody("Password changed successfully");
            user.setCanChangeCredentials(false);
            UserService.updateUser(user);
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
            String currentUsername = UserService.getUsernameForToken(token);
            User user = UserService.getUser(currentUsername);
            // Check if the user is allowed to change credentials
            if (!user.canChangeCredentials()) {
                return unauthorizedResponse();
            }
            // Update the username
            try (Connection conn = DatabaseConnector.connect()) {
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
                UserService.updateUsername(currentUsername, changeRequest.getNewUsername());

                response.setStatus(HttpStatus.OK);
                response.setBody("Username updated successfully");
                user.setCanChangeCredentials(false);
                UserService.updateUser(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            response.setBody("Error processing change username request");
        }
        return response;
    }
}