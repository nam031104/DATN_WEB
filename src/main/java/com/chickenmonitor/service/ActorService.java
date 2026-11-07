package com.chickenmonitor.service;

import com.chickenmonitor.entity.Actor;

import java.util.List;
import java.util.Optional;

public interface ActorService {
    List<Actor> getActorsByDeviceId(Long deviceId);
    Optional<Actor> getActorById(Long id);
    Actor createActor(Actor actor);
    Actor updateActor(Long id, Actor actorDetails);
    void deleteActor(Long id);
}
