package com.chickenmonitor.controller;

import com.chickenmonitor.entity.Sensor;
import com.chickenmonitor.entity.SensorData;
import com.chickenmonitor.entity.User;
import com.chickenmonitor.dto.response.SensorDataResponse;
import com.chickenmonitor.dto.response.SensorResponse;
import com.chickenmonitor.repository.DeviceRepository;
import com.chickenmonitor.repository.SensorDataRepository;
import com.chickenmonitor.repository.SensorRepository;
import com.chickenmonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class SensorController {

    private final SensorRepository sensorRepository;
    private final SensorDataRepository sensorDataRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<SensorResponse>> getSensorsByDeviceId(@PathVariable Long deviceId,
                                                                      Authentication authentication) {
        User user = currentUser(authentication);
        if (deviceRepository.findByDeviceIdAndFarmUserUserId(deviceId, user.getUserId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(sensorRepository.findByDeviceDeviceId(deviceId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SensorResponse> getSensorById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(toResponse(ownedSensor(id, authentication)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSensor(@PathVariable Long id, Authentication authentication) {
        sensorRepository.delete(ownedSensor(id, authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{sensorId}/data")
    public ResponseEntity<List<SensorDataResponse>> getSensorDataBySensorId(
            @PathVariable Long sensorId, @RequestParam(defaultValue = "week") String period,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (sensorRepository.findBySensorIdAndDeviceFarmUserUserId(sensorId, user.getUserId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        LocalDateTime from = switch (period.toLowerCase()) {
            case "day" -> LocalDateTime.now().minusDays(1);
            case "month" -> LocalDateTime.now().minusDays(30);
            case "week" -> LocalDateTime.now().minusDays(7);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khoảng thời gian không hợp lệ.");
        };
        List<SensorDataResponse> data = sensorDataRepository
                .findTop500BySensorSensorIdAndCreatedAtAfterOrderByCreatedAtDesc(sensorId, from)
                .stream()
                .sorted(java.util.Comparator.comparing(SensorData::getCreatedAt))
                .map(item -> new SensorDataResponse(item.getSensordataId(), item.getValue(), item.getUnit(), item.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(data);
    }

    private Sensor ownedSensor(Long id, Authentication authentication) {
        User user = currentUser(authentication);
        return sensorRepository.findBySensorIdAndDeviceFarmUserUserId(id, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private SensorResponse toResponse(Sensor sensor) {
        return new SensorResponse(sensor.getSensorId(), sensor.getExternalCode(), sensor.getTypeSensor(),
                sensor.getDevice().getDeviceId());
    }
}