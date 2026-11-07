package com.chickenmonitor.controller;

import com.chickenmonitor.dto.response.CameraStreamResponse;
import com.chickenmonitor.entity.Device;
import com.chickenmonitor.entity.User;
import com.chickenmonitor.repository.DeviceRepository;
import com.chickenmonitor.repository.UserRepository;
import com.chickenmonitor.service.CameraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cameras")
@RequiredArgsConstructor
public class CameraController {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final CameraService cameraService;

    @PostMapping("/{deviceId}/stream")
    public CameraStreamResponse requestStream(@PathVariable Long deviceId, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Device device = deviceRepository.findByDeviceIdAndFarmUserUserId(deviceId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return cameraService.requestStream(device);
    }
}