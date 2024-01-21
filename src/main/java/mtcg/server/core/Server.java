package mtcg.server.core;

import mtcg.server.handlers.RequestHandler;
import mtcg.server.database.*;
import mtcg.services.IUserService;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private int port;
    private DatabaseConnector databaseConnector; // Add a DatabaseConnector field

    public Server(int port) {
        this.port = port;
        this.databaseConnector = new DatabaseConnectorImpl(); // Instantiate DatabaseConnectorImpl
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected");

                    // Pass the DatabaseConnector to the ClientHandler
                    new ClientHandler(clientSocket, databaseConnector).start();

                } catch (IOException e) {
                    System.out.println("I/O error: " + e.getMessage());
                }
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

class ClientHandler extends Thread {
    private Socket socket;
    private DatabaseConnector databaseConnector; // Add a DatabaseConnector field
    private IUserService userService;

    public ClientHandler(Socket socket, DatabaseConnector databaseConnector) {
        this.socket = socket;
        this.databaseConnector = databaseConnector; // Initialize the DatabaseConnector field
    }

    public void run() {
        try {
            // Pass the DatabaseConnector to the RequestHandler
            RequestHandler requestHandler = new RequestHandler(socket, databaseConnector, userService);

            // Let RequestHandler process the request
            requestHandler.run();

        } catch (Exception ex) {
            System.out.println("Server exception handling client: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

