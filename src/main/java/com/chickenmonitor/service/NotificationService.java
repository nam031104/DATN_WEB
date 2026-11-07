package com.chickenmonitor.service;

import com.chickenmonitor.entity.Notification;

import java.util.List;

public interface NotificationService {
    List<Notification> getNotificationsByUserId(Long userId);
    Notification createNotification(Notification notification);
    void deleteNotification(Long id);
}