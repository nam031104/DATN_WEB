package com.chickenmonitor.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(
        FarmResponse farm,
        List<DeviceView> devices,
        List<SensorView> sensors,
        List<ActorView> actors
) {
    public record DeviceView(Long deviceId, String deviceCode, String name, String typeDevice, String note) {
    }

    public record SensorView(Long sensorId, String externalCode, String typeSensor, String deviceName,
                             Float value, String unit, LocalDateTime createdAt) {
    }

    public record ActorView(Long actorId, String externalCode, String typeActor, String deviceName,
                            Integer mode, Integer status, String sourceSensorCode,
                            Double thresholdMin, Double thresholdMax) {
    }
}