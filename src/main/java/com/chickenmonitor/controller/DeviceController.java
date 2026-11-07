package com.chickenmonitor.controller;

import com.chickenmonitor.dto.request.DeviceRequest;
import com.chickenmonitor.dto.request.ClaimDeviceRequest;
import com.chickenmonitor.dto.response.DeviceResponse;
import com.chickenmonitor.entity.Device;
import com.chickenmonitor.entity.Farm;
import com.chickenmonitor.entity.User;
import com.chickenmonitor.repository.DeviceRepository;
import com.chickenmonitor.repository.FarmRepository;
import com.chickenmonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class DeviceController {

    private final DeviceRepository deviceRepository;
    private final FarmRepository farmRepository;
    private final UserRepository userRepository;

    @PostMapping("/claim")
    public ResponseEntity<DeviceResponse> claimDevice(@RequestBody ClaimDeviceRequest request,
                                                      Authentication authentication) {
        if (request.deviceCode() == null || request.deviceCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần nhập mã thiết bị.");
        }
        Device device = deviceRepository.findByDeviceCodeAndFarmIsNull(request.deviceCode().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy thiết bị chưa gán với mã này."));
        device.setFarm(ownedFarm(request.farmId(), authentication));
        return ResponseEntity.ok(toResponse(deviceRepository.save(device)));
    }

    @GetMapping
    public ResponseEntity<List<DeviceResponse>> getAllDevices(Authentication authentication) {
        User user = currentUser(authentication);
        List<DeviceResponse> devices = farmRepository.findByUserUserId(user.getUserId()).stream()
                .flatMap(farm -> deviceRepository.findByFarmFarmId(farm.getFarmId()).stream())
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<DeviceResponse>> getDevicesByFarmId(@PathVariable Long farmId,
                                                                   Authentication authentication) {
        Farm farm = ownedFarm(farmId, authentication);
        return ResponseEntity.ok(deviceRepository.findByFarmFarmId(farm.getFarmId()).stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDeviceById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(toResponse(ownedDevice(id, authentication)));
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> createDevice(@RequestBody DeviceRequest request,
                                                       Authentication authentication) {
        Farm farm = ownedFarm(request.farmId(), authentication);
        Device device = Device.builder()
                .farm(farm)
                .name(requireText(request.name(), "Tên thiết bị"))
                .typeDevice(requireText(request.typeDevice(), "Loại thiết bị"))
                .note(request.note())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(deviceRepository.save(device)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponse> updateDevice(@PathVariable Long id, @RequestBody DeviceRequest request,
                                                       Authentication authentication) {
        Device device = ownedDevice(id, authentication);
        device.setName(requireText(request.name(), "Tên thiết bị"));
        device.setTypeDevice(requireText(request.typeDevice(), "Loại thiết bị"));
        device.setNote(request.note());
        if (request.farmId() != null) device.setFarm(ownedFarm(request.farmId(), authentication));
        return ResponseEntity.ok(toResponse(deviceRepository.save(device)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id, Authentication authentication) {
        deviceRepository.delete(ownedDevice(id, authentication));
        return ResponseEntity.noContent().build();
    }

    private Device ownedDevice(Long id, Authentication authentication) {
        User user = currentUser(authentication);
        return deviceRepository.findByDeviceIdAndFarmUserUserId(id, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Farm ownedFarm(Long id, Authentication authentication) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần chọn khu vực chuồng.");
        User user = currentUser(authentication);
        return farmRepository.findByFarmIdAndUserUserId(id, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " không được để trống.");
        return value.trim();
    }

    private DeviceResponse toResponse(Device device) {
        return new DeviceResponse(device.getDeviceId(), device.getFarm().getFarmId(), device.getDeviceCode(), device.getName(),
                device.getTypeDevice(), device.getNote());
    }
}