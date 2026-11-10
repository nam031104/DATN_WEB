package com.chickenmonitor.ingestion;

import com.chickenmonitor.entity.Actor;
import com.chickenmonitor.entity.Device;
import com.chickenmonitor.entity.Sensor;
import com.chickenmonitor.entity.SensorData;
import com.chickenmonitor.service.SensorAlertService;
import com.chickenmonitor.repository.ActorRepository;
import com.chickenmonitor.repository.DeviceRepository;
import com.chickenmonitor.repository.SensorDataRepository;
import com.chickenmonitor.repository.SensorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MqttIntegrationService implements ApplicationRunner, MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttIntegrationService.class);
    private static final String REGISTER_TOPIC = "chickenmonitor/+/add";
    private static final String TELEMETRY_TOPIC = "chickenmonitor/+/data";

    private final ObjectMapper objectMapper;
    private final DeviceRepository deviceRepository;
    private final SensorRepository sensorRepository;
    private final SensorDataRepository sensorDataRepository;
    private final ActorRepository actorRepository;
    private final LiveUpdateHandler liveUpdateHandler;
    private final SensorAlertService sensorAlertService;
    private final Map<Long, Integer> pendingAutomaticCommands = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final String brokerUrl;
    private final String clientId;
    private final String username;
    private final String password;
    private MqttClient client;

    public MqttIntegrationService(ObjectMapper objectMapper,
                                  DeviceRepository deviceRepository,
                                  SensorRepository sensorRepository,
                                  SensorDataRepository sensorDataRepository,
                                  ActorRepository actorRepository,
                                  LiveUpdateHandler liveUpdateHandler,
                                  SensorAlertService sensorAlertService,
                                  @Value("${app.mqtt.enabled:false}") boolean enabled,
                                  @Value("${app.mqtt.broker-url:tcp://localhost:1883}") String brokerUrl,
                                  @Value("${app.mqtt.client-id:chickenmonitor-server}") String clientId,
                                  @Value("${app.mqtt.username:}") String username,
                                  @Value("${app.mqtt.password:}") String password) {
        this.objectMapper = objectMapper;
        this.deviceRepository = deviceRepository;
        this.sensorRepository = sensorRepository;
        this.sensorDataRepository = sensorDataRepository;
        this.actorRepository = actorRepository;
        this.liveUpdateHandler = liveUpdateHandler;
        this.sensorAlertService = sensorAlertService;
        this.enabled = enabled;
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("MQTT integration is disabled. Set app.mqtt.enabled=true to connect to the broker.");
            return;
        }
        connectIfNeeded();
    }

    @Scheduled(fixedDelayString = "${app.mqtt.reconnect-delay-ms:10000}")
    public void reconnectIfNeeded() {
        if (enabled) connectIfNeeded();
    }

    private synchronized void connectIfNeeded() {
        try {
            if (client == null) client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            if (client.isConnected()) return;
            client.setCallback(this);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(false);
            options.setCleanSession(true);
            if (!username.isBlank()) {
                options.setUserName(username);
                options.setPassword(password.toCharArray());
            }
            client.connect(options);
            log.info("Connected to MQTT broker {}", brokerUrl);
        } catch (MqttException exception) {
            log.error("Unable to connect to MQTT broker {}. Application remains available without live telemetry.", brokerUrl, exception);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT connection lost: {}", cause == null ? "unknown" : cause.getMessage());
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        try {
            client.subscribe(REGISTER_TOPIC, 1);
            client.subscribe(TELEMETRY_TOPIC, 1);
            log.info("Subscribed to device registration and telemetry topics at {}", serverURI);
        } catch (MqttException exception) {
            log.error("Unable to subscribe to device topics", exception);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {
        try {
            String payload = new String(mqttMessage.getPayload(), StandardCharsets.UTF_8);
            DevicePacket packet = objectMapper.readValue(payload, DevicePacket.class);
            String topicCode = topic.split("/")[1];
            if (packet.idDevice() != null && !packet.idDevice().equals(topicCode)) {
                log.warn("Ignoring MQTT payload whose device code does not match topic {}", topic);
                return;
            }
            Device device = deviceRepository.findByDeviceCodeWithOwner(topicCode).orElse(null);
            if (device == null || device.getFarm() == null) {
                log.warn("Ignoring MQTT message for unprovisioned device code {}", topicCode);
                return;
            }
            boolean registration = topic.endsWith("/add");
            if (registration) upsertComponents(device, packet);
            List<SensorSnapshot> snapshots = saveReadings(device, packet.sensors());
            upsertActors(device, packet.actors());
            applyAutomaticRules(device, packet.sensors());
            liveUpdateHandler.sendToUser(device.getFarm().getUser().getUsername(), Map.of(
                    "type", registration ? "device-registered" : "telemetry",
                    "farmId", device.getFarm().getFarmId(),
                    "deviceId", device.getDeviceId(),
                    "sensors", snapshots,
                    "actors", packet.actors() == null ? List.of() : packet.actors()));
        } catch (Exception exception) {
            log.error("Unable to process MQTT message from {}", topic, exception);
        }
    }

    private void upsertComponents(Device device, DevicePacket packet) {
        if (packet.sensors() != null) {
            for (SensorPacket incoming : packet.sensors()) {
                if (incoming.idSensor() == null || incoming.idSensor().isBlank()) continue;
                Sensor sensor = sensorRepository.findByDeviceDeviceIdAndExternalCode(device.getDeviceId(), incoming.idSensor())
                        .orElseGet(() -> Sensor.builder().device(device).externalCode(incoming.idSensor()).build());
                if (sensor.getTypeSensor() == null || sensor.getTypeSensor().isBlank()) {
                    sensor.setTypeSensor(incoming.idSensor());
                }
                sensorRepository.save(sensor);
            }
        }
        if (packet.actors() != null) {
            for (ActorPacket incoming : packet.actors()) {
                if (incoming.idActor() == null || incoming.idActor().isBlank()) continue;
                Actor actor = actorRepository.findByDeviceDeviceIdAndExternalCode(device.getDeviceId(), incoming.idActor())
                        .orElseGet(() -> Actor.builder().device(device).externalCode(incoming.idActor()).build());
                actor.setTypeActor(incoming.typeActor() == null ? incoming.idActor() : incoming.typeActor());
                if (incoming.mode() != null) actor.setMode(incoming.mode());
                if (incoming.status() != null) actor.setStatus(incoming.status());
                actorRepository.save(actor);
            }
        }
    }

    private List<SensorSnapshot> saveReadings(Device device, List<SensorPacket> readings) {
        if (readings == null) return List.of();
        return readings.stream().filter(item -> item.idSensor() != null && item.value() != null && item.unit() != null)
                .map(item -> sensorRepository.findByDeviceDeviceIdAndExternalCode(device.getDeviceId(), item.idSensor())
                        .map(sensor -> {
                            SensorData data = SensorData.builder().sensor(sensor).value(item.value()).unit(item.unit()).build();
                            SensorData saved = sensorDataRepository.save(data);
                            sensorAlertService.check(sensor, saved.getValue(), saved.getUnit());
                            return new SensorSnapshot(sensor.getTypeSensor(), saved.getValue(), saved.getUnit(), saved.getCreatedAt());
                        }).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private void upsertActors(Device device, List<ActorPacket> actors) {
        if (actors == null) return;
        for (ActorPacket incoming : actors) {
            if (incoming.idActor() == null) continue;
            actorRepository.findByDeviceDeviceIdAndExternalCode(device.getDeviceId(), incoming.idActor()).ifPresent(actor -> {
                if (incoming.mode() != null) actor.setMode(incoming.mode());
                if (incoming.status() != null) {
                    actor.setStatus(incoming.status());
                    pendingAutomaticCommands.remove(actor.getActorId(), incoming.status());
                }
                actorRepository.save(actor);
            });
        }
    }

    private void applyAutomaticRules(Device device, List<SensorPacket> readings) {
        if (readings == null) return;
        List<Actor> actors = actorRepository.findByDeviceDeviceId(device.getDeviceId());
        for (SensorPacket reading : readings) {
            if (reading.idSensor() == null || reading.value() == null) continue;
            for (Actor actor : actors) {
                if (actor.getMode() == null || actor.getMode() != 0
                        || !reading.idSensor().equals(actor.getSourceSensorCode())
                        || actor.getThresholdMin() == null || actor.getThresholdMax() == null) continue;
                int desired = reading.value() >= actor.getThresholdMax() ? 1
                        : reading.value() <= actor.getThresholdMin() ? 0
                        : actor.getStatus() == null ? 0 : actor.getStatus();
                if ((actor.getStatus() != null && actor.getStatus() == desired)
                        || java.util.Objects.equals(pendingAutomaticCommands.get(actor.getActorId()), desired)) continue;
                try {
                    publishActorCommand(device, actor, 0, desired);
                    pendingAutomaticCommands.put(actor.getActorId(), desired);
                } catch (IllegalStateException exception) {
                    log.debug("Automatic command for actor {} is waiting for MQTT connectivity", actor.getActorId());
                }
            }
        }
    }

    public void publishActorCommand(Device device, Actor actor, int mode, int status) {
        if (!enabled || client == null || !client.isConnected()) {
            throw new IllegalStateException("MQTT broker chưa kết nối; chưa gửi được lệnh tới thiết bị.");
        }
        try {
            String topic = "chickenmonitor/" + device.getDeviceCode() + "/commands";
            String payload = objectMapper.writeValueAsString(Map.of(
                    "idDevice", device.getDeviceCode(),
                    "idActor", actor.getExternalCode(),
                    "mode", mode,
                    "status", status));
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            client.publish(topic, message);
        } catch (Exception exception) {
            throw new IllegalStateException("Không gửi được lệnh MQTT.", exception);
        }
    }

    public void publishActorModeCommand(Actor actor, int mode, String sourceSensorCode,
                                        Double thresholdMin, Double thresholdMax) {
        if (!enabled || client == null || !client.isConnected()) {
            throw new IllegalStateException("MQTT broker chưa kết nối; chưa gửi được cấu hình chế độ.");
        }
        try {
            Device device = actor.getDevice();
            Map<String, Object> command = new java.util.LinkedHashMap<>();
            command.put("idDevice", device.getDeviceCode());
            command.put("idActor", actor.getExternalCode());
            command.put("mode", mode);
            command.put("status", actor.getStatus() == null ? 0 : actor.getStatus());
            if (mode == 0) {
                command.put("sourceSensorCode", sourceSensorCode);
                command.put("thresholdMin", thresholdMin);
                command.put("thresholdMax", thresholdMax);
            }
            String topic = "chickenmonitor/" + device.getDeviceCode() + "/commands";
            MqttMessage message = new MqttMessage(objectMapper.writeValueAsBytes(command));
            message.setQos(1);
            client.publish(topic, message);
        } catch (Exception exception) {
            throw new IllegalStateException("Không gửi được cấu hình MQTT.", exception);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    @PreDestroy
    public void disconnect() {
        if (client == null || !client.isConnected()) return;
        try {
            client.disconnect();
            client.close();
        } catch (MqttException exception) {
            log.warn("Failed to close MQTT client cleanly", exception);
        }
    }

    public record DevicePacket(String idDevice, List<ActorPacket> actors, List<SensorPacket> sensors) {
    }

    public record ActorPacket(String idActor, String typeActor, Integer mode, Integer status) {
    }

    public record SensorPacket(String idSensor, Float value, String unit) {
    }

    public record SensorSnapshot(String typeSensor, Float value, String unit, java.time.LocalDateTime createdAt) {
    }
}