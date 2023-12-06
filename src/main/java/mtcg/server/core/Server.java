package mtcg.server.core;

import mtcg.server.handlers.RequestHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private int port;

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected");

                    // Handle client connection in a separate thread
                    new ClientHandler(clientSocket).start();

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

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            // Initialize RequestHandler with the client's socket
            RequestHandler requestHandler = new RequestHandler(socket);

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

