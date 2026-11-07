package com.chickenmonitor.dto.response;

public record UserResponse(Long userId, String username, String fullname, String email, String role) {
}