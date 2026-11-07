package com.chickenmonitor.dto.response;

import java.time.LocalDateTime;

public record NotificationResponse(Long notificationId, String message, Integer status, LocalDateTime createdAt) {
}