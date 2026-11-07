package com.chickenmonitor.controller;

import com.chickenmonitor.entity.Actor;
import com.chickenmonitor.dto.request.ActorCommandRequest;
import com.chickenmonitor.dto.request.ActorModeRequest;
import com.chickenmonitor.dto.response.ActorResponse;
import com.chickenmonitor.entity.User;
import com.chickenmonitor.repository.ActorRepository;
import com.chickenmonitor.repository.UserRepository;
import com.chickenmonitor.ingestion.MqttIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/actors")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ActorController {

    private final ActorRepository actorRepository;
    private final UserRepository userRepository;
    private final MqttIntegrationService mqttIntegrationService;

    @Transactional
    @PutMapping("/{id}/mode")
        public ResponseEntity<?> configureMode(@PathVariable Long id, @RequestBody ActorModeRequest request,
                           Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Actor actor = actorRepository.findByActorIdAndDeviceFarmUserUserId(id, user.getUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (request.mode() == null || (request.mode() != 0 && request.mode() != 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mode chỉ nhận 0 (tự động) hoặc 1 (thủ công).");
        }
        if (request.mode() == 0 && (request.sourceSensorCode() == null || request.sourceSensorCode().isBlank()
            || request.thresholdMin() == null || request.thresholdMax() == null
            || request.thresholdMin() >= request.thresholdMax())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Chế độ tự động cần sensor nguồn và ngưỡng thấp nhỏ hơn ngưỡng cao.");
        }
        String sourceCode = request.mode() == 0 ? request.sourceSensorCode().trim() : null;
        Double thresholdMin = request.mode() == 0 ? request.thresholdMin() : null;
        Double thresholdMax = request.mode() == 0 ? request.thresholdMax() : null;
        try {
            mqttIntegrationService.publishActorModeCommand(actor, request.mode(), sourceCode, thresholdMin, thresholdMax);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        }
        actor.setMode(request.mode());
        actor.setSourceSensorCode(sourceCode);
        actor.setThresholdMin(thresholdMin);
        actor.setThresholdMax(thresholdMax);
        Actor saved = actorRepository.save(actor);
        return ResponseEntity.ok(java.util.Map.of("actorId", saved.getActorId(), "mode", saved.getMode()));
        }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<ActorResponse>> getActorsByDeviceId(@PathVariable Long deviceId,
                                                                   Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return ResponseEntity.ok(actorRepository.findByDeviceDeviceIdAndDeviceFarmUserUserId(deviceId, user.getUserId())
            .stream().map(actor -> new ActorResponse(actor.getActorId(), actor.getExternalCode(),
                actor.getTypeActor(), actor.getMode(), actor.getStatus(), actor.getDevice().getDeviceId()))
            .toList());
    }

    @PostMapping("/{id}/command")
    public ResponseEntity<?> sendCommand(@PathVariable Long id, @RequestBody ActorCommandRequest request,
                                         Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Actor actor = actorRepository.findByActorIdAndDeviceFarmUserUserId(id, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (actor.getMode() == null || actor.getMode() != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chuyển thiết bị sang thủ công trước khi gửi lệnh bật/tắt.");
        }
        if (request.mode() == null || request.mode() != 1
                || request.status() == null || (request.status() != 0 && request.status() != 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lệnh bật/tắt chỉ nhận mode=1 và status 0 hoặc 1.");
        }
        try {
            mqttIntegrationService.publishActorCommand(actor.getDevice(), actor, request.mode(), request.status());
            return ResponseEntity.accepted().body(java.util.Map.of(
                    "message", "Đã gửi lệnh; trạng thái sẽ cập nhật khi thiết bị phản hồi.",
                    "actorId", actor.getActorId(), "mode", request.mode(), "status", request.status()));
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        }
    }

}
