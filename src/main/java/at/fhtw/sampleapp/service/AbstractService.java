package at.fhtw.sampleapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractService {
    private ObjectMapper objectMapper;

    public AbstractService() {
        this.objectMapper = new ObjectMapper();
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
