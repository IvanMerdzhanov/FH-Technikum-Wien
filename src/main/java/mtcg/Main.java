package mtcg;

import mtcg.server.core.Server;

public class Main {
    public static void main(String[] args) {
        int serverPort = 8000; // Set your server port

        // Create and start the server
        Server server = new Server(serverPort);
        server.start(); // This could also be run in a new thread if non-blocking behavior is desired

        System.out.println("Server is running on port " + serverPort);
        // The server is now listening for requests

    }
}
