package com.chickenmonitor.dto.response;

public record SensorResponse(Long sensorId, String externalCode, String typeSensor, Long deviceId) {
}