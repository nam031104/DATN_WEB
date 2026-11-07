package com.chickenmonitor.dto.request;

import java.time.LocalTime;

public record ScheduleRequest(Long actorId, LocalTime executionTime, Integer status, Integer mode, String note) {
}