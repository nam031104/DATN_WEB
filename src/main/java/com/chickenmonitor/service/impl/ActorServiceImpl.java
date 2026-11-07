package com.chickenmonitor.service.impl;

import com.chickenmonitor.entity.Actor;
import com.chickenmonitor.repository.ActorRepository;
import com.chickenmonitor.service.ActorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ActorServiceImpl implements ActorService {

    private final ActorRepository actorRepository;

    @Override
    public List<Actor> getActorsByDeviceId(Long deviceId) {
        return actorRepository.findByDeviceDeviceId(deviceId);
    }

    @Override
    public Optional<Actor> getActorById(Long id) {
        return actorRepository.findById(id);
    }

    @Override
    public Actor createActor(Actor actor) {
        return actorRepository.save(actor);
    }

    @Override
    public Actor updateActor(Long id, Actor actorDetails) {
        Actor actor = actorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Actor với ID: " + id));

        actor.setTypeActor(actorDetails.getTypeActor());
        actor.setMode(actorDetails.getMode());
        actor.setStatus(actorDetails.getStatus());
        return actorRepository.save(actor);
    }

    @Override
    public void deleteActor(Long id) {
        if (!actorRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy Actor với ID: " + id);
        }
        actorRepository.deleteById(id);
    }
}