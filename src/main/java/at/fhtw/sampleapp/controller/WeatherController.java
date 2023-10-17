package at.fhtw.sampleapp.controller;

import at.fhtw.httpserver.http.ContentType;
import at.fhtw.httpserver.http.HttpStatus;
import at.fhtw.httpserver.http.Method;
import at.fhtw.httpserver.server.Request;
import at.fhtw.httpserver.server.Response;
import at.fhtw.httpserver.server.RestController;
import at.fhtw.sampleapp.service.WeatherService;

public class WeatherController implements RestController {
    private final WeatherService weatherService;

    public WeatherController() {
        this.weatherService = new WeatherService();
    }

    @Override
    public Response handleRequest(Request request) {
        System.out.println("handleRequest");
        if (request.getMethod() == Method.GET &&
            request.getPathParts().size() > 1) {
            return this.weatherService.getWeather(request.getPathParts().get(1));
        } else if (request.getMethod() == Method.GET) {
            return this.weatherService.getWeatherPerRepository();
        } else if (request.getMethod() == Method.POST) {
            return this.weatherService.addWeather(request);
        }

        return new Response(
                HttpStatus.BAD_REQUEST,
                ContentType.JSON,
                "[]"
        );
    }
}
