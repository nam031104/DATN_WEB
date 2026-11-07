package com.chickenmonitor.dto.request;

import java.util.List;

public record ProvisionDeviceRequest(String deviceCode, String name, String typeDevice, String note,
                                     Long userId, Long farmId, List<SensorTemplate> sensors,
                                     List<ActorTemplate> actors) {
    public record SensorTemplate(String externalCode, String typeSensor) {
    }

    public record ActorTemplate(String externalCode, String typeActor, Integer mode, Integer status) {
    }
}