package com.chickenmonitor.dto.response;

import java.time.LocalTime;

public record ScheduleResponse(Long scheduleId, Long actorId, String actorType, String deviceName,
                               LocalTime executionTime, Integer status, Integer mode, String note) {
}