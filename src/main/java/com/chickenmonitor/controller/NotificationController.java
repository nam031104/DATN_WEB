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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page, Authentication authentication) {
        User user = currentUser(authentication);
        if (page < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page không hợp lệ");
        var items = notificationRepository.findByUserUserId(user.getUserId(),
                PageRequest.of(page, 100, Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "notificationId"))));
        return ResponseEntity.ok(items.stream().map(this::toResponse).toList());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable Long id, Authentication authentication) {
        Notification notification = ownedNotification(id, authentication);
        notification.setStatus(1);
        return ResponseEntity.ok(toResponse(notificationRepository.save(notification)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<java.util.Map<String, Integer>> markAllRead(Authentication authentication) {
        User user = currentUser(authentication);
        List<Notification> unread = notificationRepository
                .findByUserUserIdOrderByCreatedAtDesc(user.getUserId()).stream()
                .filter(item -> item.getStatus() == null || item.getStatus() != 1)
                .toList();
        unread.forEach(item -> item.setStatus(1));
        notificationRepository.saveAll(unread);
        return ResponseEntity.ok(java.util.Map.of("updated", unread.size()));
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
