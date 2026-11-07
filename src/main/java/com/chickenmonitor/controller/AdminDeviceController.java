package com.chickenmonitor.controller;

import com.chickenmonitor.dto.request.ProvisionDeviceRequest;
import com.chickenmonitor.dto.response.DeviceResponse;
import com.chickenmonitor.entity.Actor;
import com.chickenmonitor.entity.Device;
import com.chickenmonitor.entity.Farm;
import com.chickenmonitor.entity.Sensor;
import com.chickenmonitor.entity.User;
import com.chickenmonitor.repository.ActorRepository;
import com.chickenmonitor.repository.DeviceRepository;
import com.chickenmonitor.repository.FarmRepository;
import com.chickenmonitor.repository.SensorRepository;
import com.chickenmonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin/devices")
@RequiredArgsConstructor
public class AdminDeviceController {

    private final DeviceRepository deviceRepository;
    private final FarmRepository farmRepository;
    private final UserRepository userRepository;
    private final SensorRepository sensorRepository;
    private final ActorRepository actorRepository;

        @GetMapping
        public ResponseEntity<?> getProvisionedDevices() {
        return ResponseEntity.ok(deviceRepository.findAll().stream()
            .map(device -> new DeviceResponse(device.getDeviceId(),
                device.getFarm() == null ? null : device.getFarm().getFarmId(),
                device.getDeviceCode(), device.getName(), device.getTypeDevice(), device.getNote()))
            .toList());
        }

    @PostMapping("/provision")
    @Transactional
    public ResponseEntity<?> provision(@RequestBody ProvisionDeviceRequest request) {
        String code = requireText(request.deviceCode(), "Mã thiết bị");
        if (deviceRepository.existsByDeviceCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã thiết bị đã tồn tại.");
        }
        boolean hasUser = request.userId() != null;
        boolean hasFarm = request.farmId() != null;
        if (hasUser != hasFarm) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần chọn đồng thời user và chuồng, hoặc để trống cả hai.");
        }
        User user = hasUser ? userRepository.findById(request.userId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng.")) : null;
        Farm farm = hasFarm ? farmRepository.findByFarmIdAndUserUserId(request.farmId(), user.getUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chuồng không thuộc người dùng đã chọn.")) : null;
        Device device = deviceRepository.save(Device.builder()
                .farm(farm)
                .deviceCode(code)
                .name(requireText(request.name(), "Tên thiết bị"))
                .typeDevice(requireText(request.typeDevice(), "Loại thiết bị"))
                .note(request.note())
                .build());
        if (request.sensors() != null) {
            request.sensors().forEach(item -> sensorRepository.save(Sensor.builder()
                    .device(device)
                    .externalCode(requireText(item.externalCode(), "Mã cảm biến"))
                    .typeSensor(requireText(item.typeSensor(), "Loại cảm biến"))
                    .build()));
        }
        if (request.actors() != null) {
            request.actors().forEach(item -> actorRepository.save(Actor.builder()
                    .device(device)
                    .externalCode(requireText(item.externalCode(), "Mã actor"))
                    .typeActor(requireText(item.typeActor(), "Loại actor"))
                    .mode(item.mode() == null ? 0 : item.mode())
                    .status(item.status() == null ? 0 : item.status())
                    .build()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of(
            "deviceId", device.getDeviceId(), "deviceCode", code,
            "assigned", farm != null));
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " không được để trống.");
        }
        return value.trim();
    }
}