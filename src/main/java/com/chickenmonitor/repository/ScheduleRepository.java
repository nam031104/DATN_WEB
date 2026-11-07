package com.chickenmonitor.repository;

import com.chickenmonitor.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalTime;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    // Lấy danh sách lịch trình của một thiết bị chấp hành
    List<Schedule> findByActorActorId(Long actorId);

    List<Schedule> findByActorDeviceFarmUserUserIdOrderByExecutionTimeAsc(Long userId);

    java.util.Optional<Schedule> findByScheduleIdAndActorDeviceFarmUserUserId(Long scheduleId, Long userId);

    List<Schedule> findByExecutionTimeBetween(LocalTime from, LocalTime to);
}
