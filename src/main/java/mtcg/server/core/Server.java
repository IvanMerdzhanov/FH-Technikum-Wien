package mtcg.server.core;

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

    public static void main(String[] args) {
        int port = 8000; // Set the port you want to listen on
        Server server = new Server(port);
        server.start();
    }
}

class ClientHandler extends Thread {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (InputStream input = socket.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input));
             OutputStream output = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(output, true)) {

            // Example of reading a single line from the client
            String text = reader.readLine();
            System.out.println("Message from client: " + text);

            // Example of sending a response back to the client
            writer.println("Server acknowledges message: " + text);

            socket.close();

        } catch (IOException ex) {
            System.out.println("Server exception handling client: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
