package com.chickenmonitor.dto.request;

public record ActorModeRequest(Integer mode, String sourceSensorCode, Double thresholdMin, Double thresholdMax) {
}