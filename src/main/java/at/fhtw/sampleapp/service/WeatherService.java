package at.fhtw.sampleapp.service;

import at.fhtw.httpserver.http.HttpStatus;
import at.fhtw.httpserver.server.Request;
import at.fhtw.httpserver.server.Response;

public class WeatherService extends AbstractService {

    public WeatherService() {

    }

    // GET /weather(:id
    public Response getWeather(String id)
    {
            return new Response(HttpStatus.NOT_IMPLEMENTED);
    }
    // GET /weather
    public Response getWeather() {
        return new Response(HttpStatus.NOT_IMPLEMENTED);
    }

    // POST /weather
    public Response addWeather(Request request) {
        return new Response(HttpStatus.NOT_IMPLEMENTED);
    }

    // GET /weather
    // gleich wie "public Response getWeather()" nur mittels Repository
    public Response getWeatherPerRepository() {
        return new Response(HttpStatus.NOT_IMPLEMENTED);
    }
}
