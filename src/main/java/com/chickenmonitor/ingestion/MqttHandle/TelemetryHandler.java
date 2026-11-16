package com.chickenmonitor.ingestion.MqttHandle;

import com.chickenmonitor.dto.MqttDto.TelemetryDto;
import com.chickenmonitor.entity.Device;
import com.chickenmonitor.entity.SensorData;
import com.chickenmonitor.ingestion.LiveUpdateHandler;
import com.chickenmonitor.ingestion.MqttIntegrationService;
import com.chickenmonitor.ingestion.MqttMessageHandler;
import com.chickenmonitor.repository.DeviceRepository;
import com.chickenmonitor.repository.SensorDataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import java.util.List;


@Component
@RequiredArgsConstructor
public class TelemetryHandler implements MqttMessageHandler {
    private final ObjectMapper objectMapper;
    private final Logger log = LoggerFactory.getLogger(TelemetryHandler.class);
    private final DeviceRepository deviceRepository;
    private final SensorDataRepository sensorDataRepository;
    private final LiveUpdateHandler liveUpdateHandler;

    @Override
    @Transactional
    public void handle(String topic, String payload) {
        try {
            TelemetryDto packet = objectMapper.readValue(payload,TelemetryDto.class);
            Device device = deviceRepository.findByDeviceCodeWithOwner(packet.idDevice()).orElse(null);
            if (device == null || device.getFarm() == null) {
                log.warn("Ignoring MQTT message for unprovisioned device code {}", packet.idDevice());
                return;
            }
            List<SensorData> sensorDataList = packet.toSensorData(device);

            if (sensorDataList.isEmpty()) {
                return;
            }

            sensorDataRepository.saveAll(sensorDataList);

            log.info(
                    "Saved {} sensor records for device {}",
                    sensorDataList.size(),
                    packet.idDevice()
            );

            // Chuyển Entity thành dữ liệu JSON đơn giản
            List<Map<String, Object>> sensors =
                    sensorDataList.stream()
                            .map(data -> Map.<String, Object>of(
                                    "sensordataId", data.getSensordataId(),
                                    "sensorCode", data.getSensor().getExternalCode(),
                                    "typeSensor", data.getSensor().getTypeSensor(),
                                    "value", data.getValue(),
                                    "unit", data.getUnit(),
                                    "createdAt", data.getCreatedAt()
                            ))
                            .toList();

            String username =
                    device.getFarm().getUser().getUsername();

            Map<String, Object> message = Map.of(
                    "type", "telemetry",
                    "farmId", device.getFarm().getFarmId(),
                    "deviceId", device.getDeviceId(),
                    "sensors", sensors
            );

            liveUpdateHandler.sendToUser(username, message);

        } catch (Exception exception) {
            log.error("Unable to process MQTT message from {}", topic, exception);
        }

    }

}

