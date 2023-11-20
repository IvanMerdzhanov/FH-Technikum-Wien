package at.fhtw.sampleapp.controller.weather;

import at.fhtw.sampleapp.model.Weather;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeatherTest {

    @Test
    void testWeatherServiceGetCompleteList() throws Exception {
        URL url = new URL("http://localhost:10001/weather");
        URLConnection urlConnection = url.openConnection();
        InputStream inputStream = urlConnection.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        try {
            //Weather[] weatherList = new ObjectMapper().readValue(bufferedReader.readLine(), Weather[].class);
            //assertEquals(3, weatherList.length);
            List<Weather> weatherList = new ObjectMapper().readValue(bufferedReader.readLine(), new TypeReference<List<Weather>>(){});
            assertEquals(3, weatherList.size());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        bufferedReader.close();
    }

    @Test
    void testWeatherServiceGetByIdCheckString() throws Exception {
        URL url = new URL("http://localhost:10001/weather/1");
        URLConnection urlConnection = url.openConnection();
        InputStream inputStream = urlConnection.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        assertEquals("{\"id\":1,\"region\":\"Europe\",\"city\":\"Vienna\",\"temperature\":9.0}", bufferedReader.readLine());

        bufferedReader.close();
    }

    @Test
    void testWeatherServiceGetById() throws Exception {
        URL url = new URL("http://localhost:10001/weather/1");
        URLConnection urlConnection = url.openConnection();
        InputStream inputStream = urlConnection.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        try {
            Weather weather = new ObjectMapper().readValue(bufferedReader.readLine(), Weather.class);
            assertEquals(1, weather.getId());
            assertEquals("Vienna", weather.getCity());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        bufferedReader.close();
    }

}