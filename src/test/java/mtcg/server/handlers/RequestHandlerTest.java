package mtcg.server.handlers;

import mtcg.models.Card;
import mtcg.models.Deck;
import mtcg.models.User;
import mtcg.models.UserStats;
import mtcg.services.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import mtcg.server.database.DatabaseConnector;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.junit.runner.RunWith;


import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

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

    private ByteArrayOutputStream outputCapture;
    private IUserService mockUserService;


    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Setup mock behaviors for database connection
        when(databaseConnector.connect()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1); // Mock successful update
        mockUserService = mock(IUserService.class);

        outputCapture = new ByteArrayOutputStream();
        when(clientSocket.getOutputStream()).thenReturn(new OutputStream() {
            @Override
            public void write(int b) {
                outputCapture.write(b);
            }
        });

        // Create an instance of RequestHandler with mocked dependencies
        requestHandler = new RequestHandler(clientSocket, databaseConnector, mockUserService);

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

}
