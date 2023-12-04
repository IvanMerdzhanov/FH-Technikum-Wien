package mtcg.server.handlers;
import mtcg.models.*;
import mtcg.server.http.*;
import java.io.*;
import java.net.Socket;
import mtcg.server.models.*;
import mtcg.server.util.JsonSerializer;


public class RequestHandler implements Runnable {
    private final Socket clientSocket;

    public RequestHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream())) {

            // Parse the request
            HttpRequest request = new HttpRequest(in);

            // Determine the response based on the URI
            HttpResponse response = new HttpResponse();
            if ("/battle".equals(request.getUri()) && "POST".equalsIgnoreCase(request.getMethod())) {
                response = handleBattleRequest(request);
            } else {
                // Handle other requests
                response.setStatus(HttpStatus.NOT_FOUND);
                response.setBody("Not Found");
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