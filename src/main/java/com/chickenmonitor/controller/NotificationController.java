package com.chickenmonitor.controller;

import com.chickenmonitor.dto.response.NotificationResponse;
import com.chickenmonitor.entity.Notification;
import com.chickenmonitor.entity.User;
import com.chickenmonitor.repository.NotificationRepository;
import com.chickenmonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(Authentication authentication) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(notificationRepository.findTop100ByUserUserIdOrderByCreatedAtDesc(user.getUserId())
                .stream().map(this::toResponse).toList());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable Long id, Authentication authentication) {
        Notification notification = ownedNotification(id, authentication);
        notification.setStatus(1);
        return ResponseEntity.ok(toResponse(notificationRepository.save(notification)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id, Authentication authentication) {
        notificationRepository.delete(ownedNotification(id, authentication));
        return ResponseEntity.noContent().build();
    }

    private Notification ownedNotification(Long id, Authentication authentication) {
        User user = currentUser(authentication);
        return notificationRepository.findById(id).filter(notification -> notification.getUser().getUserId().equals(user.getUserId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getNotificationId(), notification.getMessage(),
                notification.getStatus(), notification.getCreatedAt());
    }
}
