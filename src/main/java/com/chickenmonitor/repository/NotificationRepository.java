package com.chickenmonitor.repository;

import com.chickenmonitor.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Lấy danh sách thông báo của một user, sắp xếp mới nhất lên đầu
    List<Notification> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findTop100ByUserUserIdOrderByCreatedAtDesc(Long userId);
}
