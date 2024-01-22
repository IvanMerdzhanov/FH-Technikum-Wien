package mtcg.server.handlers;

import mtcg.models.*;
import mtcg.services.IPackageService;
import mtcg.services.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import mtcg.server.database.DatabaseConnector;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.junit.runner.RunWith;


import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
@RunWith(PowerMockRunner.class)
@PrepareForTest({RequestHandler.class})
class RequestHandlerTest {

    @Mock
    private Socket clientSocket;
    @Mock
    private DatabaseConnector databaseConnector;
    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement mockPreparedStatement;
    @Mock
    private ResultSet mockResultSet;
    private RequestHandler requestHandler;
    @Mock
    private IPackageService mockPackageService;

    private ByteArrayOutputStream outputCapture;
    private IUserService mockUserService;

    @Mock private InputStream inputStream;
    @Mock private OutputStream outputStream;


    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Setup mock behaviors for database connection
        when(databaseConnector.connect()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1); // Mock successful update
        mockUserService = mock(IUserService.class);
        mockPackageService = mock(IPackageService.class);

        outputCapture = new ByteArrayOutputStream();
        when(clientSocket.getOutputStream()).thenReturn(new OutputStream() {
            @Override
            public void write(int b) {
                outputCapture.write(b);
            }
        });

        // Setup for capturing output stream
       // when(clientSocket.getOutputStream()).thenReturn(outputStream);
        // when(clientSocket.getInputStream()).thenReturn(inputStream);

        // Create an instance of RequestHandler with mocked dependencies
        requestHandler = new RequestHandler(clientSocket, databaseConnector, mockUserService, mockPackageService);

    }

    private void setUpHttpRequest(String httpRequest) throws IOException {
        InputStream input = new ByteArrayInputStream(httpRequest.getBytes());
        when(clientSocket.getInputStream()).thenReturn(input);
    }

    @Test
    void testHandleRegisterRequest() throws Exception {
        // Set up the specific HTTP request for this test
        String httpRequest = "POST /register HTTP/1.1\r\nContent-Length: ...\r\n\r\n{\"username\":\"newUser1\",\"password\":\"password123\"}\r\n";
        setUpHttpRequest(httpRequest);

        // Execute the method to be tested
        requestHandler.run();

        // Verify the output
        String response = outputCapture.toString();
        assertTrue(response.contains("User registered successfully"), "Expected response to indicate successful registration");
    }

    @Test
    void testRegisterWithExistingUsername() throws Exception {
        // Setup mock ResultSet to simulate user already exists
        when(mockResultSet.next()).thenReturn(true); // Simulate a user already exists with the given username

        // Set up the specific HTTP request for this test
        String httpRequest = "POST /register HTTP/1.1\r\nContent-Length: ...\r\n\r\n{\"username\":\"newUser1\",\"password\":\"password123\"}\r\n";
        setUpHttpRequest(httpRequest);

        // Run the RequestHandler
        requestHandler.run();

        // Verify the response
        String response = outputCapture.toString();
        assertTrue(response.contains("Username already taken"), "Expected response to indicate username is already taken");
    }

    @Test
    void testRegisterWithInvalidData() throws Exception {
        // Example of an invalid request - missing password field
        String invalidHttpRequest = "POST /register HTTP/1.1\r\nContent-Length: ...\r\n\r\n{\"username\":\"newUser\"}\r\n";
        setUpHttpRequest(invalidHttpRequest);

        // Run the RequestHandler
        requestHandler.run();

        // Verify the response
        String response = outputCapture.toString();
        assertTrue(response.contains("Invalid registration data"), "Expected response to indicate invalid registration data");
    }

    @Test
    void testUserLoginSuccess() throws Exception {
        // Generate a real hashed password for the mock (use your actual password hashing logic)
        String realHashedPassword = BCrypt.hashpw("password123", BCrypt.gensalt());

        // Mock a successful login HTTP request with correct credentials
        String loginHttpRequest = "POST /login HTTP/1.1\r\nContent-Length: ...\r\n\r\n{\"username\":\"newUser1\",\"password\":\"password123\"}\r\n";
        setUpHttpRequest(loginHttpRequest);

        // Mock database behavior for successful login
        when(mockResultSet.next()).thenReturn(true); // Simulate user exists
        when(mockResultSet.getString("password")).thenReturn(realHashedPassword); // Return the real hashed password

        // Run the RequestHandler
        requestHandler.run();

        // Verify the response
        String response = outputCapture.toString();
        assertTrue(response.contains("Login successful"), "Expected response to indicate successful login");
    }
    @Test
    public void testStartBattleWithoutEnoughPlayers() throws Exception {
        // Mock the behavior of getActiveSessionsCount() method
        when(mockUserService.getActiveSessionsCount()).thenReturn(1);

        System.out.println("Setting up HTTP request for battle with sufficient players");
        String battleRequestJson = "{\"tokenPlayerOne\":\"token1\", \"tokenPlayerTwo\":\"token2\"}";
        String httpRequest = "POST /battle HTTP/1.1\r\nContent-Length: ...\r\n\r\n" + battleRequestJson;
        setUpHttpRequest(httpRequest);

        System.out.println("Running requestHandler for sufficient players test");
        requestHandler.run();

        // Verify the response
        String response = outputCapture.toString();
        System.out.println("Response for battle with sufficient players: " + response);
        // Verify the response indicates not enough players
        assertTrue(response.contains("Not enough players for battle"), "Expected response to indicate not enough players for battle");

    }
    @Test
    void testStartBattleWithSufficientPlayers() throws Exception {
        // Mock UserService methods
        when(mockUserService.getActiveSessionsCount()).thenReturn(2); // Simulate at least 2 active sessions
        when(mockUserService.isActiveSession("token1")).thenReturn(true);
        when(mockUserService.isActiveSession("token2")).thenReturn(true);
        when(mockUserService.getUsernameForToken("token1")).thenReturn("newUser1");
        when(mockUserService.getUsernameForToken("token2")).thenReturn("newUser2");

        User mockUser1 = mock(User.class);
        User mockUser2 = mock(User.class);
        Deck mockDeck1 = mock(Deck.class);
        Deck mockDeck2 = mock(Deck.class);

        // Set up mock behavior for the decks
        List<Card> mockCards1 = new ArrayList<>(); // Add mock cards as needed
        List<Card> mockCards2 = new ArrayList<>(); // Add mock cards as needed
        when(mockDeck1.getCards()).thenReturn(mockCards1);
        when(mockDeck2.getCards()).thenReturn(mockCards2);

        // Ensure the mock users return these deck objects
        when(mockUser1.getDeck()).thenReturn(mockDeck1);
        when(mockUser2.getDeck()).thenReturn(mockDeck2);

        UserStats mockStats1 = mock(UserStats.class);
        UserStats mockStats2 = mock(UserStats.class);

        when(mockUser1.getUserStats()).thenReturn(mockStats1);
        when(mockUser2.getUserStats()).thenReturn(mockStats2);

        // Return the mock User objects when UserService.getUser() is called
        when(mockUserService.getUser("newUser1")).thenReturn(mockUser1);
        when(mockUserService.getUser("newUser2")).thenReturn(mockUser2);

        // Mock the HTTP request for starting a battle
        System.out.println("Setting up HTTP request for battle with enough players");
        String battleRequestJson = "{\"tokenPlayerOne\":\"token1\", \"tokenPlayerTwo\":\"token2\"}";
        String httpRequest = "POST /battle HTTP/1.1\r\nContent-Length: ...\r\n\r\n" + battleRequestJson;
        setUpHttpRequest(httpRequest);

        System.out.println("Running requestHandler for not enough players test");
        requestHandler.run();

        // Verify the response
        String response = outputCapture.toString();
        System.out.println("Response for battle without enough players: " + response);
    }
    @Test
    void testHandleLogoutRequest_Success() throws Exception {
        // Setup
        when(mockUserService.isActiveSession("token1")).thenReturn(true);
        when(mockUserService.getUsernameForToken("token1")).thenReturn("newUser1");

        User mockUser1 = mock(User.class);
        Deck mockDeck1 = mock(Deck.class);

        // Set up mock behavior for the decks
        List<Card> mockCards1 = new ArrayList<>(); // Add mock cards as needed
        when(mockDeck1.getCards()).thenReturn(mockCards1);

        // Ensure the mock users return these deck objects
        when(mockUser1.getDeck()).thenReturn(mockDeck1);

        UserStats mockStats1 = mock(UserStats.class);

        when(mockUser1.getUserStats()).thenReturn(mockStats1);

        // Return the mock User objects when UserService.getUser() is called
        when(mockUserService.getUser("newUser1")).thenReturn(mockUser1);

        // Mock the HTTP request for starting a battle
        System.out.println("Setting up HTTP request for logout");
       // String logoutRequestJson = "{\"tokenPlayerOne\":\"token1\", \"tokenPlayerTwo\":\"token2\"}";
        String httpRequest = "POST /logout HTTP/1.1\r\n" +
                "Authorization: Bearer token1\r\n" +
                "\r\n";
        setUpHttpRequest(httpRequest);

        System.out.println("Running requestHandler for logout");
        requestHandler.run();

        // Verify the response
        String response = outputCapture.toString();
        System.out.println("Response for logout: " + response);

    }
    @Test
    void testRequestPackageWithSufficientCoins() throws Exception {
        // Setup HTTP request for getpackage with sufficient coins
        String httpRequest = "POST /getpackage HTTP/1.1\r\n" +
                "Authorization: Bearer validToken\r\n" +
                "\r\n";
        setUpHttpRequest(httpRequest);

        // Mock user behavior
        when(mockUserService.isActiveSession("validToken")).thenReturn(true);
        when(mockUserService.getUsernameForToken("validToken")).thenReturn("newUser");
        User mockUser = mock(User.class);
        when(mockUser.getCoins()).thenReturn(5); // Sufficient coins
        when(mockUserService.getUser("newUser")).thenReturn(mockUser);

        // Mock package service behavior
        when(mockPackageService.getPackageCards()).thenReturn(new ArrayList<>()); // Mock package cards

        // Run the RequestHandler
        requestHandler.run();

        // Verify the response
        String response = outputCapture.toString();
        assertTrue(response.contains("Package acquired successfully"), "Expected response to indicate successful package acquisition");
    }
    @Test
    void testRequestPackageWithInsufficientCoins() throws Exception {
        // Setup HTTP request for getpackage with insufficient coins
        String httpRequest = "POST /getpackage HTTP/1.1\r\n" +
                "Authorization: Bearer validToken\r\n" +
                "\r\n";
        setUpHttpRequest(httpRequest);

        // Mock user behavior
        when(mockUserService.isActiveSession("validToken")).thenReturn(true);
        when(mockUserService.getUsernameForToken("validToken")).thenReturn("newUser");
        User mockUser = mock(User.class);
        when(mockUser.getCoins()).thenReturn(4); // Insufficient coins
        when(mockUserService.getUser("newUser")).thenReturn(mockUser);

        // Run the RequestHandler
        requestHandler.run();

        // Verify the response
        String response = outputCapture.toString();
        assertTrue(response.contains("Insufficient coins"), "Expected response to indicate insufficient coins for package acquisition");
    }
    @Test
    void testHandleShowMyCardsRequest_Success() throws Exception {
        // Set up the specific HTTP request for this test
        String httpRequest = "GET /showmycards HTTP/1.1\r\nAuthorization: Bearer validToken\r\n\r\n";
        setUpHttpRequest(httpRequest);

        // Mock user behavior
        when(mockUserService.isActiveSession("validToken")).thenReturn(true);
        when(mockUserService.getUsernameForToken("validToken")).thenReturn("newUser");

        User mockUser = mock(User.class);
        Deck mockDeck = mock(Deck.class);
        List<Card> mockCardList = Arrays.asList(mock(Card.class), mock(Card.class)); // Mock some cards
        when(mockUser.getStack()).thenReturn(mockCardList);
        when(mockUser.getDeck()).thenReturn(mockDeck);
        when(mockDeck.getCards()).thenReturn(mockCardList); // Mock deck cards, adjust as needed

        when(mockUserService.getUser("newUser")).thenReturn(mockUser);

        // Run the RequestHandler
        requestHandler.run();

        // Verify the response
        String response = outputCapture.toString();
        assertTrue(response.contains("Stack:"), "Expected response to contain 'Stack:'");
        assertTrue(response.contains("Deck:"), "Expected response to contain 'Deck:'");
    }
    @Test
    void testSelectCardsForDeckSuccess() throws Exception {
        // Setup HTTP request for selectCard
        String selectCardsJson = "{\"cardIndexes\": [1, 2, 3, 5]}";
        String httpRequest = "POST /selectCard HTTP/1.1\r\n" +
                "Authorization: Bearer validToken\r\n" +
                "Content-Length: " + selectCardsJson.getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n" +
                selectCardsJson;
        setUpHttpRequest(httpRequest);

        // Mock user behavior
        when(mockUserService.isActiveSession("validToken")).thenReturn(true);
        when(mockUserService.getUsernameForToken("validToken")).thenReturn("newUser");
        User mockUser = mock(User.class);
        when(mockUserService.getUser("newUser")).thenReturn(mockUser);

        Deck mockDeck = mock(Deck.class);
        when(mockUser.getDeck()).thenReturn(mockDeck);

        // Mock user stack and offers
        List<Card> mockStack = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            mockStack.add(mock(Card.class));
        }
        when(mockUser.getStack()).thenReturn(mockStack);

        when(mockUser.getStack()).thenReturn(mockStack);
        when(mockUser.getOffers()).thenReturn(new ArrayList<>());

        // Run the RequestHandler
        requestHandler.run();

        // Verify the response
        String response = outputCapture.toString();
        assertTrue(response.contains("Cards selected successfully"), "Expected response to indicate successful card selection");

        // Verify the deck manipulation
        verify(mockDeck).clear();
        verify(mockDeck, times(4)).addCard(any(Card.class));
    }
    @Test
    void testSelectInvalidCardIndexesForDeck() throws Exception {
        // Setup HTTP request with invalid card indexes
        String selectCardsJson = "{\"cardIndexes\": [10, 20]}"; // Assuming these indexes are invalid
        String httpRequest = "POST /selectCard HTTP/1.1\r\n" +
                "Authorization: Bearer validToken\r\n" +
                "Content-Length: " + selectCardsJson.getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n" +
                selectCardsJson;
        setUpHttpRequest(httpRequest);

        // Mock user behavior
        when(mockUserService.isActiveSession("validToken")).thenReturn(true);
        when(mockUserService.getUsernameForToken("validToken")).thenReturn("newUser");
        User mockUser = mock(User.class);
        when(mockUserService.getUser("newUser")).thenReturn(mockUser);

        Deck mockDeck = mock(Deck.class);
        when(mockUser.getDeck()).thenReturn(mockDeck);

        // Mock user stack and offers
        List<Card> mockStack = new ArrayList<>();
        for (int i = 0; i < 5; i++) { // Assume the user only has 5 cards
            mockStack.add(mock(Card.class));
        }
        when(mockUser.getStack()).thenReturn(mockStack);
        when(mockUser.getOffers()).thenReturn(new ArrayList<>());

        // Run the RequestHandler
        requestHandler.run();

        // Verify the response for invalid card index
        String response = outputCapture.toString();
        assertTrue(response.contains("Invalid card selection"), "Expected response to indicate invalid card selection");

        // Verify no cards were added to the deck
        verify(mockDeck, never()).addCard(any(Card.class));
    }
    @Test
    void testViewWalletAfterTransactions() throws Exception {
        // Simulate the user performing a transaction (e.g., purchasing a package)
        // Setup HTTP request for package purchase
        String packageRequest = "POST /getpackage HTTP/1.1\r\n" +
                "Authorization: Bearer validToken\r\n" +
                "\r\n";
        setUpHttpRequest(packageRequest);

        // Mock user behavior for package purchase
        User mockUser = mock(User.class);
        when(mockUserService.isActiveSession("validToken")).thenReturn(true);
        when(mockUserService.getUsernameForToken("validToken")).thenReturn("newUser");
        when(mockUser.getCoins()).thenReturn(10); // Initial coin balance
        when(mockUserService.getUser("newUser")).thenReturn(mockUser);

        // Mock package service behavior
        when(mockPackageService.getPackageCards()).thenReturn(new ArrayList<>());

        // Execute the package purchase request
        requestHandler.run();

        // Reset output stream for wallet request
        outputCapture.reset();

        // Setup HTTP request for wallet balance check
        String walletRequest = "GET /wallet HTTP/1.1\r\n" +
                "Authorization: Bearer validToken\r\n" +
                "\r\n";
        setUpHttpRequest(walletRequest);

        // Mock updated user behavior to reflect coin balance after transaction
        when(mockUser.getCoins()).thenReturn(5); // Assuming 5 coins are spent

        // Execute the wallet request
        requestHandler.run();

        // Verify wallet response
        String walletResponse = outputCapture.toString();
        assertTrue(walletResponse.contains("Coins: 5"), "Expected wallet response to reflect updated coin balance after transaction");
    }
    @Test
    void testShowUserRecordAfterBattles() throws Exception {
        // Setup HTTP request for showing user record
        String httpRequest = "GET /showRecord HTTP/1.1\r\n" +
                "Authorization: Bearer validToken\r\n" +
                "\r\n";
        setUpHttpRequest(httpRequest);

        // Mock user behavior
        when(mockUserService.isActiveSession("validToken")).thenReturn(true);
        when(mockUserService.getUsernameForToken("validToken")).thenReturn("newUser");

        // Assuming the constructor of UserStats takes four arguments
        UserStats mockStats = new UserStats(10, 5, 2, 120); // Wins, Losses, Draws, ELO

        User mockUser = mock(User.class);
        when(mockUser.getUserStats()).thenReturn(mockStats);
        when(mockUserService.getUser("newUser")).thenReturn(mockUser);

        // Run the RequestHandler
        requestHandler.run();

        // Verify the response
        String response = outputCapture.toString();
        assertTrue(response.contains("Record for newUser"));
        assertTrue(response.contains("Wins: 10"));
        assertTrue(response.contains("Losses: 5"));
        assertTrue(response.contains("Draws: 2"));
        assertTrue(response.contains("ELO Rating: 120"), "Expected response to contain user battle statistics");
    }
    @Test
    void testHandleShowScoreboardRequest() throws Exception {
        // Mock the database behavior
        when(databaseConnector.connect()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        // Set up mock ResultSet data
        when(mockResultSet.next()).thenReturn(true, true, false); // Two rows of data, then end
        when(mockResultSet.getString("username")).thenReturn("User1", "User2");
        when(mockResultSet.getInt("total_wins")).thenReturn(5, 3);
        when(mockResultSet.getInt("total_losses")).thenReturn(2, 4);
        when(mockResultSet.getInt("total_draws")).thenReturn(1, 1);
        when(mockResultSet.getInt("elo_rating")).thenReturn(150, 140);

        // Setup HTTP request for the scoreboard
        String httpRequest = "POST /scoreboard HTTP/1.1\r\n\r\n";
        setUpHttpRequest(httpRequest);

        // Run the RequestHandler
        requestHandler.run();

        // Verify the response
        String response = outputCapture.toString();
         System.out.println(response);
        assertTrue(response.contains("User1") && response.contains("User2"), "Expected response to contain scoreboard data");
        assertTrue(response.contains("150") && response.contains("140"), "Expected response to contain ELO ratings");
    }

    @Test
    void testCreateCardForCardTradeOffer() throws Exception {
        // Setup mock HttpRequest
        //!!!THE INDEX SHOULD BE +1 BECAUSE OF THe DECREMENTION IN THE FUNCTION!!!
        String httpRequest = "POST /createTradeOffer HTTP/1.1\r\nAuthorization: Bearer validToken\r\n\r\n" +
                "{\"offeringUsername\": \"newUser1\", \"receivingUsername\": \"newUser2\", \"offeredCardIndex\": 2, \"requestedType\": \"Spell\", \"minimumDamage\": 70, \"typeOfOffer\": \"card-for-card\"}";
        setUpHttpRequest(httpRequest);

        // Mock user behavior
        User mockOfferingUser = mock(User.class);
        User mockReceivingUser = mock(User.class);

        // Create a mock stack of cards
        List<Card> mockStack = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            mockStack.add(mock(Card.class));
        }

        when(mockUserService.isActiveSession("validToken")).thenReturn(true);
        when(mockUserService.getUsernameForToken("validToken")).thenReturn("newUser1");
        when(mockUserService.getUser("newUser1")).thenReturn(mockOfferingUser);
        when(mockUserService.getUser("newUser2")).thenReturn(mockReceivingUser);
        when(mockReceivingUser.getOffers()).thenReturn(new ArrayList<>());

        when(mockOfferingUser.getUsername()).thenReturn("newUser1");
        when(mockOfferingUser.getStack()).thenReturn(mockStack);
        when(mockOfferingUser.getDeck()).thenReturn(new Deck());


        // Run the RequestHandler
        requestHandler.run();

        // Verify the response
        String response = outputCapture.toString();
        assertTrue(response.contains("Trade offer created successfully"), "Expected response to indicate successful trade offer creation");
    }
    @Test
    void testCreateCoinsForCardTradeOffer() throws Exception {
        // Setup mock HttpRequest for a coins-for-card trade offer
        String httpRequest = "POST /createTradeOffer HTTP/1.1\r\nAuthorization: Bearer validToken\r\n\r\n" +
                "{\"offeringUsername\": \"newUser1\", \"receivingUsername\": \"newUser2\", \"offeredCoins\": 5, \"requestedType\": \"Any\", \"minimumDamage\": 50, \"typeOfOffer\": \"coins-for-card\"}";
        setUpHttpRequest(httpRequest);

        // Mock user behavior
        User mockOfferingUser = mock(User.class);
        User mockReceivingUser = mock(User.class);
        when(mockUserService.isActiveSession("validToken")).thenReturn(true);
        when(mockUserService.getUsernameForToken("validToken")).thenReturn("newUser1");
        when(mockUserService.getUser("newUser1")).thenReturn(mockOfferingUser);
        when(mockUserService.getUser("newUser2")).thenReturn(mockReceivingUser);
        when(mockReceivingUser.getOffers()).thenReturn(new ArrayList<>());

        // Assuming the offering user has enough coins
        when(mockOfferingUser.getCoins()).thenReturn(10);

        // Run the RequestHandler
        requestHandler.run();

        // Verify the response
        String response = outputCapture.toString();
        assertTrue(response.contains("Trade offer created successfully"), "Expected response to indicate successful trade offer creation");
    }
    @Test
    void testAcceptValidTradeOffer() throws Exception {
        // Setup HTTP request for accepting a trade offer
        String httpRequest = "POST /acceptOffer HTTP/1.1\r\nAuthorization: Bearer validToken\r\n\r\n" +
                "{\"offerIndex\": 1}";
        setUpHttpRequest(httpRequest);

        // Mock user behavior
        User mockUser = mock(User.class);
        User offeringUser = mock(User.class); // Mock offering user
        Card offeredCard = mock(Card.class); // Mock offered card

        // Create a mock trading offer with all necessary fields
        Trading mockTradingOffer = new Trading(offeringUser, mockUser, offeredCard, 5, "Any", 50, "card-for-coins");

        when(mockUserService.isActiveSession("validToken")).thenReturn(true);
        when(mockUserService.getUsernameForToken("validToken")).thenReturn("newUser");
        when(mockUserService.getUser("newUser")).thenReturn(mockUser);

        // Mock the user's coin count to meet the trade requirements
        when(mockUser.getCoins()).thenReturn(5); // or more

        // Mock the user's offers list to include the mocked trading offer
        when(mockUser.getOffers()).thenReturn(List.of(mockTradingOffer));

        // Run the RequestHandler
        requestHandler.run();

        // Verify the response
        String response = outputCapture.toString();
       // System.out.println(response);
        assertTrue(response.contains("Card and coins trade completed successfully"), "Expected response to indicate successful acceptance of trade offer");
    }
    @Test
    void testDeclineTradeOffer() throws Exception {
        // Setup HTTP request for declining a trade offer
        String httpRequest = "POST /declineOffer HTTP/1.1\r\nAuthorization: Bearer validToken\r\n\r\n" +
                "{\"offerIndex\": 1}";
        setUpHttpRequest(httpRequest);

        // Mock user behavior
        User mockUser = mock(User.class);
        Trading mockTradingOffer = mock(Trading.class); // Mock the trading offer
        List<Trading> offers = new ArrayList<>();
        offers.add(mockTradingOffer); // Add mock offer to list

        when(mockUserService.isActiveSession("validToken")).thenReturn(true);
        when(mockUserService.getUsernameForToken("validToken")).thenReturn("newUser");
        when(mockUserService.getUser("newUser")).thenReturn(mockUser);
        when(mockUser.getOffers()).thenReturn(offers);

        // Run the RequestHandler
        requestHandler.run();

        // Verify the response
        String response = outputCapture.toString();
        assertTrue(response.contains("Trade offer declined successfully"), "Expected response to indicate successful decline of trade offer");
        assertTrue(mockUser.getOffers().isEmpty(), "Expected the offer list to be empty after declining the offer");
    }

}