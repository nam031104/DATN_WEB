package com.chickenmonitor.repository;

import com.chickenmonitor.entity.Actor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActorRepository extends JpaRepository<Actor, Long> {
    // Lấy danh sách thiết bị chấp hành thuộc về một thiết bị gốc
    List<Actor> findByDeviceDeviceId(Long deviceId);

    Optional<Actor> findByActorIdAndDeviceFarmUserUserId(Long actorId, Long userId);

    List<Actor> findByDeviceDeviceIdAndDeviceFarmUserUserId(Long deviceId, Long userId);

    Optional<Actor> findByDeviceDeviceIdAndExternalCode(Long deviceId, String externalCode);
}
