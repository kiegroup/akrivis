package org.kie.akrivis.quarkus;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class YardServiceMock {


    private String result;
    private Map<String, Object> payload;

    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}
