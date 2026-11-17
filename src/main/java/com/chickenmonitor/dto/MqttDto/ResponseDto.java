package com.chickenmonitor.dto.MqttDto;

public record ResponseDto(String idDevice, Integer mode, boolean status, String code) {
}
