package com.chickenmonitor.dto.response;

public record ActorResponse(Long actorId, String externalCode, String typeActor,
                            Integer mode, Integer status, Long deviceId) {
}