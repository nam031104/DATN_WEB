package com.chickenmonitor.dto.MqttDto;

import com.chickenmonitor.entity.Device;
import com.chickenmonitor.entity.Sensor;
import com.chickenmonitor.entity.SensorData;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record TelemetryDto(String idDevice, List<SensorPacket> sensors) {
    public List<SensorData> toSensorData(Device device) {

        if (sensors == null || sensors.isEmpty()) {
            return List.of();
        }

        // Tạo Map để tìm Sensor theo code nhanh hơn
        Map<String, Sensor> sensorMap = device.getSensors()
                .stream()
                .collect(Collectors.toMap(
                        Sensor::getExternalCode,
                        Function.identity()
                ));

        return sensors.stream()
                .map(packet -> {
                    Sensor sensor = sensorMap.get(packet.code());

                    if (sensor == null) {
                        throw new IllegalArgumentException(
                                "Sensor not found: " + packet.code()
                                        + " for device: " + idDevice
                        );
                    }

                    return SensorData.builder()
                            .sensor(sensor)
                            .value(packet.value())
                            .unit(packet.unit())
                            .build();
                })
                .toList();
    }
}
