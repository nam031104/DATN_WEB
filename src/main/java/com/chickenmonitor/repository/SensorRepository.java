package com.chickenmonitor.repository;

import com.chickenmonitor.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {
    // Lấy danh sách cảm biến thuộc về một thiết bị gốc
    List<Sensor> findByDeviceDeviceId(Long deviceId);

    Optional<Sensor> findBySensorIdAndDeviceFarmUserUserId(Long sensorId, Long userId);

    Optional<Sensor> findByDeviceDeviceIdAndExternalCode(Long deviceId, String externalCode);
}
