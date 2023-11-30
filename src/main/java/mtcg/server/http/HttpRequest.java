package mtcg.server.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private String method;
    private String uri;
    private Map<String, String> headers;
    private String body;

    public HttpRequest(BufferedReader reader) throws IOException {
        headers = new HashMap<>();
        parseRequest(reader);
    }

    private void parseRequest(BufferedReader reader) throws IOException {
        // Parse the request line
        String line = reader.readLine();
        if (line != null) {
            String[] requestLine = line.split(" ");
            method = requestLine[0];
            uri = requestLine[1];
        }

        // Parse headers
        line = reader.readLine();
        while (line != null && !line.isEmpty()) {
            String[] header = line.split(": ");
            headers.put(header[0], header[1]);
            line = reader.readLine();
        }

        // If POST method, there might be a body
        if ("POST".equalsIgnoreCase(method)) {
            // Read the body
            StringBuilder bodyBuilder = new StringBuilder();
            while (reader.ready()) {
                bodyBuilder.append((char) reader.read());
            }
            body = bodyBuilder.toString();
        }
    }

    // Getters
    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}
