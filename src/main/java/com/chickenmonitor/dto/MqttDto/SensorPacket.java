package com.chickenmonitor.dto.MqttDto;

public record SensorPacket(String code, Float value, String unit) {
}