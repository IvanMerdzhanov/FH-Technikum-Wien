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

            Card offeredCard = null;
            if (tradeOfferRequest.getCardIndex() != null) {
                int cardIndex = tradeOfferRequest.getCardIndex() - 1; // Adjust for 1-based index from client
                if (cardIndex >= 0 && cardIndex < offeringUser.getStack().size()) {
                    offeredCard = offeringUser.getStack().get(cardIndex);
                }
            }

            Trading tradeOffer;
            if (offeredCard != null && tradeOfferRequest.getCoins() > 0) {
                tradeOffer = new Trading(offeringUser, receivingUser, offeredCard, tradeOfferRequest.getCoins());
            } else if (offeredCard != null) {
                tradeOffer = new Trading(offeringUser, receivingUser, offeredCard);
            } else {
                tradeOffer = new Trading(offeringUser, receivingUser, tradeOfferRequest.getCoins());
            }

            if (!validateOffer(offeringUser, tradeOffer)) {
                response.setStatus(HttpStatus.BAD_REQUEST);
                response.setBody("Invalid offer, card not owned or insufficient coins");
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

    private boolean validateOffer(User offeringUser, Trading tradeOffer) {
        if (tradeOffer.getOfferedCard() != null && !offeringUser.getStack().contains(tradeOffer.getOfferedCard())) {
            return false; // User doesn't own the card
        }
        if (tradeOffer.getOfferedCoins() > 0 && offeringUser.getCoins() < tradeOffer.getOfferedCoins()) {
            return false; // User doesn't have enough coins
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
            offerDetails.append("Offer from: ").append(offer.getOfferingUser().getUsername());
            offerDetails.append(" to: ").append(offer.getReceivingUser().getUsername());

            if (offer.getOfferedCard() != null) {
                offerDetails.append(", Card: ").append(offer.getOfferedCard().getName());
                offerDetails.append(" (ID: ").append(offer.getOfferedCard().getId()).append(")");
            }

            if (offer.getOfferedCoins() > 0) {
                if (offer.getOfferedCard() != null) {
                    offerDetails.append(" and ");
                } else {
                    offerDetails.append(", ");
                }
                offerDetails.append("Coins: ").append(offer.getOfferedCoins());
            }

            offerDetails.append(", Status: ").append(offer.getStatus()).append("\n");
        }

        response.setBody(offerDetails.toString());

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

        // Transfer the card if present in the offer
        if (offer.getOfferedCard() != null) {
            transferCardToStack(offeringUser, receivingUser, offer.getOfferedCard());
        }

        // Transfer coins if present in the offer
        if (offer.getOfferedCoins() > 0) {
            transferCoins(offeringUser, receivingUser, offer.getOfferedCoins());
        }

        offer.setStatus(Trading.TradeStatus.ACCEPTED);
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
}