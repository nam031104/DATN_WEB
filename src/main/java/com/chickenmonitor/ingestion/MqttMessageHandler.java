package com.chickenmonitor.ingestion;

public interface MqttMessageHandler {

    void handle(String topic, String payload);
}

