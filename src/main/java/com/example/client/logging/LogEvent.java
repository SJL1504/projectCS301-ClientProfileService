package com.example.client.logging;

import java.time.Instant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

public class LogEvent {
    private String crud;
    private String attribute;
    private String beforeValue;
    private String afterValue;
    private String agentId;
    private String clientId;
    private Instant dateTime;
    private String email;

    public LogEvent(String crud, String attribute, String beforeValue, String afterValue,
                    String agentId, String clientId, String email) {
        this.crud = crud;
        this.attribute = attribute;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.agentId = agentId;
        this.clientId = clientId;
        this.dateTime = Instant.now();
        this.email = email;
    }

    public LogEvent() {
        
    }

    public String getCrud() {
        return crud;
    }

    public void setCrud(String crud) {
        this.crud = crud;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getBeforeValue() {
        return beforeValue;
    }

    public void setBeforeValue(String beforeValue) {
        this.beforeValue = beforeValue;
    }

    public String getAfterValue() {
        return afterValue;
    }

    public void setAfterValue(String afterValue) {
        this.afterValue = afterValue;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Instant getDateTime() {
        return dateTime;
    }

    public void setDateTime(Instant dateTime) {
        this.dateTime = dateTime;
    }

    public String getEmail(){
        return email;
    }

    public void setEmail(String email){
        this.email = email;
    }

    // convert LogEvent to JSON
    public String toJson() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); // handles java.time types (Instant)
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert LogEvent to JSON", e);
        }
    }
}
