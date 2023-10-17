package at.fhtw.httpserver.server;

public interface RestController {
    Response handleRequest(Request request);
}
