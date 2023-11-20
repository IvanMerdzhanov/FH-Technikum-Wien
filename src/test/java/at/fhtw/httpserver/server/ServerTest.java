package at.fhtw.httpserver.server;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {

    @Test
    void testEchoServer() throws Exception {
        URL url = new URL("http://localhost:10001/echo?id=24");
        URLConnection urlConnection = url.openConnection();
        urlConnection.setDoOutput(true);
        OutputStream outputStream = urlConnection.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream);
        printWriter.write("Hello Underworld!");
        printWriter.close();
        InputStream inputStream = urlConnection.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        assertEquals("Echo-Hello Underworld!", bufferedReader.readLine());

        bufferedReader.close();
    }
}