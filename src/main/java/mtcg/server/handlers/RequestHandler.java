package mtcg.server.handlers;
import mtcg.server.http.*;
import java.io.*;
import java.net.Socket;

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

            // Determine the response
            HttpResponse response = new HttpResponse();
            switch (request.getMethod()) {
                case "GET":
                    // Handle GET request
                    break;
                case "POST":
                    // Handle POST request
                    break;
                // Include other HTTP methods as necessary
                default:
                    response.setStatus(HttpStatus.NOT_FOUND);
                    response.setBody("Not Found");
                    break;
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
}