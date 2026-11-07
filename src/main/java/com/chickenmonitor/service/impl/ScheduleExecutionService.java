package com.chickenmonitor.service.impl;

import com.chickenmonitor.entity.Schedule;
import com.chickenmonitor.ingestion.MqttIntegrationService;
import com.chickenmonitor.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ScheduleExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleExecutionService.class);
    private final ScheduleRepository scheduleRepository;
    private final MqttIntegrationService mqttIntegrationService;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void executeDueSchedules() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        LocalDate today = LocalDate.now();
        for (Schedule schedule : scheduleRepository.findByExecutionTimeBetween(now, now)) {
            if (today.equals(schedule.getLastExecutedDate())) continue;
            try {
                mqttIntegrationService.publishActorCommand(
                        schedule.getActor().getDevice(), schedule.getActor(), 1,
                        schedule.getStatus() == null ? 0 : schedule.getStatus());
                schedule.setLastExecutedDate(today);
                schedule.setIsExecuted(true);
                scheduleRepository.save(schedule);
            } catch (IllegalStateException exception) {
                log.debug("Schedule {} is waiting for MQTT connectivity", schedule.getScheduleId());
            }
        }
    }
}