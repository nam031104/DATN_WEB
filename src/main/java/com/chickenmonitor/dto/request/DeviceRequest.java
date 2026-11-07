package com.chickenmonitor.dto.request;

public record DeviceRequest(Long farmId, String name, String typeDevice, String note) {
}