package com.chickenmonitor.dto.response;

import java.time.LocalDateTime;

public record SensorDataResponse(Long id, Float value, String unit, LocalDateTime createdAt) {
}