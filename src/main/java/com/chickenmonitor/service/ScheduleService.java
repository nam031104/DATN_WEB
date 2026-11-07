package com.chickenmonitor.service;

import com.chickenmonitor.entity.Schedule;

import java.util.List;
import java.util.Optional;

public interface ScheduleService {
    List<Schedule> getSchedulesByActorId(Long actorId);
    Optional<Schedule> getScheduleById(Long id);
    Schedule createSchedule(Schedule schedule);
    Schedule updateSchedule(Long id, Schedule scheduleDetails);
    void deleteSchedule(Long id);
}
