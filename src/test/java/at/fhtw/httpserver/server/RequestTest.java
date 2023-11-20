package at.fhtw.httpserver.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestTest {

    @Test
    void getPathname() {
    }

    @Test
    void testGetParamsWithId() {
        Request request = new Request();
        request.setPathname("/echo/1");
        request.setParams("id=99");

        assertEquals("id=99", request.getParams());
    }

    @Test
    void testGetServiceRouteWithSlash() {
        Request request = new Request();
        request.setPathname("/");

        assertNull(request.getServiceRoute());
    }

    @Test
    void testGetServiceRouteWithRoute() {
        Request request = new Request();
        request.setPathname("/echo");

        assertEquals("/echo", request.getServiceRoute());
    }

    @Test
    void testGetServiceRouteWithSubRoute() {
        Request request = new Request();
        request.setPathname("/echo/1/cards");

        assertEquals("/echo", request.getServiceRoute());
        assertEquals("1", request.getPathParts().get(1));
        assertEquals("cards", request.getPathParts().get(2));
    }
}