package com.chickenmonitor.controller;

import com.chickenmonitor.dto.request.ScheduleRequest;
import com.chickenmonitor.dto.response.ScheduleResponse;
import com.chickenmonitor.entity.Actor;
import com.chickenmonitor.entity.Schedule;
import com.chickenmonitor.entity.User;
import com.chickenmonitor.repository.ActorRepository;
import com.chickenmonitor.repository.ScheduleRepository;
import com.chickenmonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ScheduleController {

    private final ScheduleRepository scheduleRepository;
    private final ActorRepository actorRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getSchedules(Authentication authentication) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(scheduleRepository.findByActorDeviceFarmUserUserIdOrderByExecutionTimeAsc(user.getUserId())
                .stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(@RequestBody ScheduleRequest request,
                                                           Authentication authentication) {
        Actor actor = ownedActor(request.actorId(), authentication);
        if (request.executionTime() == null || request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần chọn giờ và trạng thái bật/tắt.");
        }
        Schedule schedule = Schedule.builder().actor(actor).executionTime(request.executionTime())
                .status(request.status() == 1 ? 1 : 0).mode(1).isExecuted(false).note(request.note()).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(scheduleRepository.save(schedule)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduleResponse> updateSchedule(@PathVariable Long id,
                                                           @RequestBody ScheduleRequest request,
                                                           Authentication authentication) {
        Schedule schedule = ownedSchedule(id, authentication);
        schedule.setExecutionTime(request.executionTime());
        schedule.setStatus(request.status() == 1 ? 1 : 0);
        schedule.setIsExecuted(false);
        schedule.setNote(request.note());
        if (request.actorId() != null) schedule.setActor(ownedActor(request.actorId(), authentication));
        return ResponseEntity.ok(toResponse(scheduleRepository.save(schedule)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id, Authentication authentication) {
        scheduleRepository.delete(ownedSchedule(id, authentication));
        return ResponseEntity.noContent().build();
    }

    private Schedule ownedSchedule(Long id, Authentication authentication) {
        return scheduleRepository.findByScheduleIdAndActorDeviceFarmUserUserId(id, currentUser(authentication).getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Actor ownedActor(Long id, Authentication authentication) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần chọn thiết bị.");
        return actorRepository.findByActorIdAndDeviceFarmUserUserId(id, currentUser(authentication).getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private ScheduleResponse toResponse(Schedule schedule) {
        return new ScheduleResponse(schedule.getScheduleId(), schedule.getActor().getActorId(),
                schedule.getActor().getTypeActor(), schedule.getActor().getDevice().getName(),
                schedule.getExecutionTime(), schedule.getStatus(), schedule.getMode(), schedule.getNote());
    }
}
