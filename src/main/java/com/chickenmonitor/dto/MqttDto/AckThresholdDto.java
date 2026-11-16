package com.chickenmonitor.dto.MqttDto;

public record AckThresholdDto(String idDevice, double thresholdMax, double thresholdMin, String code) {
}
