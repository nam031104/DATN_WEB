package com.chickenmonitor.dto.response;

public record DeviceResponse(Long deviceId, Long farmId, String deviceCode,
							 String name, String typeDevice, String note) {
}