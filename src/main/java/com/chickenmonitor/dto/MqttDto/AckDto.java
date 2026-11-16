package com.chickenmonitor.dto.MqttDto;


import java.util.List;

public record AckDto(String idDevice,  Integer mode, boolean status, String code) {
}
