package mtcg.server.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class JsonSerializer {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> T deserialize(String json, Class<T> clazz) {
        try {
            System.out.println("Deserializing JSON: " + json);
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            System.err.println("Error deserializing JSON: " + json);
            e.printStackTrace();
            return null;
        }
    }
}