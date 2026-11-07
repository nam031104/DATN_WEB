package com.chickenmonitor.repository;

import com.chickenmonitor.entity.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {
    // Lấy lịch sử dữ liệu của một cảm biến, sắp xếp theo thời gian mới nhất lên đầu
    List<SensorData> findBySensorSensorIdOrderByCreatedAtDesc(Long sensorId);

    Optional<SensorData> findFirstBySensorSensorIdOrderByCreatedAtDesc(Long sensorId);

    List<SensorData> findTop500BySensorSensorIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long sensorId, LocalDateTime createdAt);
}
