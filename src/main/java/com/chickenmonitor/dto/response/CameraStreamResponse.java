package com.chickenmonitor.dto.response;

import java.time.Instant;

public record CameraStreamResponse(String streamUrl, Instant expiresAt) {
}