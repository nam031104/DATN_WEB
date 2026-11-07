package com.chickenmonitor.controller;

import com.chickenmonitor.dto.response.DashboardResponse;
import com.chickenmonitor.dto.response.FarmResponse;
import com.chickenmonitor.entity.Actor;
import com.chickenmonitor.entity.Device;
import com.chickenmonitor.entity.Farm;
import com.chickenmonitor.entity.Sensor;
import com.chickenmonitor.entity.SensorData;
import com.chickenmonitor.entity.User;
import com.chickenmonitor.repository.ActorRepository;
import com.chickenmonitor.repository.DeviceRepository;
import com.chickenmonitor.repository.FarmRepository;
import com.chickenmonitor.repository.SensorDataRepository;
import com.chickenmonitor.repository.SensorRepository;
import com.chickenmonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final FarmRepository farmRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final SensorRepository sensorRepository;
    private final ActorRepository actorRepository;
    private final SensorDataRepository sensorDataRepository;

    @GetMapping
    public DashboardResponse getDashboard(@RequestParam(required = false) Long farmId,
                                          Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        List<Farm> farms = farmRepository.findByUserUserId(user.getUserId());
        Farm farm = farmId == null
                ? farms.stream().findFirst().orElse(null)
                : farmRepository.findByFarmIdAndUserUserId(farmId, user.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (farm == null) {
            return new DashboardResponse(null, List.of(), List.of(), List.of());
        }

        List<Device> devices = deviceRepository.findByFarmFarmId(farm.getFarmId());
        List<DashboardResponse.DeviceView> deviceViews = new ArrayList<>();
        List<DashboardResponse.SensorView> sensorViews = new ArrayList<>();
        List<DashboardResponse.ActorView> actorViews = new ArrayList<>();

        for (Device device : devices) {
            deviceViews.add(new DashboardResponse.DeviceView(
                    device.getDeviceId(), device.getDeviceCode(), device.getName(), device.getTypeDevice(), device.getNote()));
            for (Sensor sensor : sensorRepository.findByDeviceDeviceId(device.getDeviceId())) {
                SensorData latest = sensorDataRepository
                        .findFirstBySensorSensorIdOrderByCreatedAtDesc(sensor.getSensorId()).orElse(null);
                sensorViews.add(new DashboardResponse.SensorView(
                        sensor.getSensorId(), sensor.getExternalCode(), sensor.getTypeSensor(), device.getName(),
                        latest == null ? null : latest.getValue(),
                        latest == null ? null : latest.getUnit(),
                        latest == null ? null : latest.getCreatedAt()));
            }
            for (Actor actor : actorRepository.findByDeviceDeviceId(device.getDeviceId())) {
                actorViews.add(new DashboardResponse.ActorView(
                        actor.getActorId(), actor.getExternalCode(), actor.getTypeActor(), device.getName(),
                        actor.getMode(), actor.getStatus(), actor.getSourceSensorCode(),
                        actor.getThresholdMin(), actor.getThresholdMax()));
            }
        }

        FarmResponse farmView = new FarmResponse(
                farm.getFarmId(), farm.getName(), farm.getLocation(), farm.getDescription());
        return new DashboardResponse(farmView, deviceViews, sensorViews, actorViews);
    }
}