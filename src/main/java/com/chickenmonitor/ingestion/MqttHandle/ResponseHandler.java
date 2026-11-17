package com.chickenmonitor.ingestion.MqttHandle;

import com.chickenmonitor.dto.MqttDto.AckDto;
import com.chickenmonitor.dto.MqttDto.ResponseDto;
import com.chickenmonitor.entity.Actor;
import com.chickenmonitor.entity.Device;
import com.chickenmonitor.ingestion.LiveUpdateHandler;
import com.chickenmonitor.ingestion.MqttMessageHandler;
import com.chickenmonitor.repository.ActorRepository;
import com.chickenmonitor.repository.DeviceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ResponseHandler implements MqttMessageHandler {
    private final ObjectMapper objectMapper;
    private final Logger log = LoggerFactory.getLogger(ResponseHandler.class);
    private final DeviceRepository deviceRepository;
    private final LiveUpdateHandler liveUpdateHandler;
    private final ActorRepository actorRepository;

    @Override
    @Transactional
    public void handle(String topic, String payload) {
        try {
            ResponseDto packet = objectMapper.readValue(payload,ResponseDto.class);
            Device device = deviceRepository.findByDeviceCodeWithOwner(packet.idDevice()).orElse(null);
            if (device == null || device.getFarm() == null) {
                log.warn("Ignoring MQTT message for unprovisioned device code {}", packet.idDevice());
                return;
            }

            Actor actor = actorRepository.findByExternalCode(packet.code())
                    .orElseThrow(() -> new Exception("Không tìm thấy Actor"));

            actor.setMode(packet.mode());
            actor.setStatus(packet.status() ? 1 : 0);

            actorRepository.save(actor);

            log.info("Saved Actor");

            Map<String, Object> actorData = Map.of(
                    "actorId", actor.getActorId(),
                    "externalCode", actor.getExternalCode() == null ? "" : actor.getExternalCode(),
                    "typeActor", actor.getTypeActor(),
                    "mode", actor.getMode(),
                    "status", actor.getStatus()
            );

            liveUpdateHandler.sendToUser(
                    device.getFarm().getUser().getUsername(),
                    Map.of(
                            "type", "response",
                            "farmId", device.getFarm().getFarmId(),
                            "deviceId", device.getDeviceId(),
                            "actor", actorData
                    )
            );

        } catch (Exception exception) {
            log.error("Unable to process MQTT message from {}", topic, exception);
        }

    }
}
