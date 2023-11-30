package mtcg.server.http;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpResponse {
    private HttpStatus status;
    private Map<String, String> headers;
    private String body;

    public HttpResponse() {
        this.status = HttpStatus.OK; // Default status
        this.headers = new HashMap<>();
        this.body = "";
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void setBody(String body) {
        this.body = body;
        // Automatically update the Content-Length header
        headers.put("Content-Length", String.valueOf(body.length()));
    }

    public String buildResponse() {
        String responseLine = "HTTP/1.1 " + status.getCode() + " " + status.getReason() + "\r\n";
        String responseHeaders = headers.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\r\n")) + "\r\n";
        String responseBody = "\r\n" + body;
        return responseLine + responseHeaders + responseBody;
    }
}
