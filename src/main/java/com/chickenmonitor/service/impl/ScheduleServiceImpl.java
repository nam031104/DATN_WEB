package com.chickenmonitor.service.impl;

import com.chickenmonitor.entity.Schedule;
import com.chickenmonitor.repository.ScheduleRepository;
import com.chickenmonitor.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;

    @Override
    public List<Schedule> getSchedulesByActorId(Long actorId) {
        return scheduleRepository.findByActorActorId(actorId);
    }

    @Override
    public Optional<Schedule> getScheduleById(Long id) {
        return scheduleRepository.findById(id);
    }

    @Override
    public Schedule createSchedule(Schedule schedule) {
        return scheduleRepository.save(schedule);
    }

    @Override
    public Schedule updateSchedule(Long id, Schedule scheduleDetails) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Schedule với ID: " + id));

        schedule.setExecutionTime(scheduleDetails.getExecutionTime());
        schedule.setStatus(scheduleDetails.getStatus());
        schedule.setMode(scheduleDetails.getMode());
        schedule.setIsExecuted(scheduleDetails.getIsExecuted());
        schedule.setNote(scheduleDetails.getNote());
        return scheduleRepository.save(schedule);
    }

    @Override
    public void deleteSchedule(Long id) {
        if (!scheduleRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy Schedule với ID: " + id);
        }
        scheduleRepository.deleteById(id);
    }
}
