package com.chickenmonitor.controller;

import com.chickenmonitor.dto.response.FarmResponse;
import com.chickenmonitor.repository.FarmRepository;
import com.chickenmonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminFarmController {

    private final UserRepository userRepository;
    private final FarmRepository farmRepository;

    @GetMapping("/{userId}/farms")
    public ResponseEntity<?> getUserFarms(@PathVariable Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng.");
        }
        return ResponseEntity.ok(farmRepository.findByUserUserId(userId).stream()
                .map(farm -> new FarmResponse(farm.getFarmId(), farm.getName(), farm.getLocation(), farm.getDescription()))
                .toList());
    }
}