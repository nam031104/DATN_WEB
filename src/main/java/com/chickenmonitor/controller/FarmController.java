package com.chickenmonitor.controller;

import com.chickenmonitor.dto.request.FarmRequest;
import com.chickenmonitor.dto.response.FarmResponse;
import com.chickenmonitor.entity.Farm;
import com.chickenmonitor.entity.User;
import com.chickenmonitor.repository.FarmRepository;
import com.chickenmonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/farms")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class FarmController {

    private final FarmRepository farmRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<FarmResponse>> getAllFarms(Authentication authentication) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(farmRepository.findByUserUserId(user.getUserId()).stream()
                .map(this::toResponse)
                .toList());
    }

    @PostMapping
    public ResponseEntity<FarmResponse> createFarm(@RequestBody FarmRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        Farm farm = Farm.builder()
                .user(user)
                .name(requireName(request.name()))
                .location(request.location())
                .description(request.description())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(farmRepository.save(farm)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FarmResponse> updateFarm(@PathVariable Long id, @RequestBody FarmRequest request,
                                                   Authentication authentication) {
        Farm farm = ownedFarm(id, authentication);
        farm.setName(requireName(request.name()));
        farm.setLocation(request.location());
        farm.setDescription(request.description());
        return ResponseEntity.ok(toResponse(farmRepository.save(farm)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFarm(@PathVariable Long id, Authentication authentication) {
        farmRepository.delete(ownedFarm(id, authentication));
        return ResponseEntity.noContent().build();
    }

    private Farm ownedFarm(Long id, Authentication authentication) {
        User user = currentUser(authentication);
        return farmRepository.findByFarmIdAndUserUserId(id, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên chuồng không được để trống.");
        }
        return name.trim();
    }

    private FarmResponse toResponse(Farm farm) {
        return new FarmResponse(farm.getFarmId(), farm.getName(), farm.getLocation(), farm.getDescription());
    }
}