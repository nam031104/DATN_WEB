package com.chickenmonitor.service;

import com.chickenmonitor.entity.Sensor;

import java.util.List;
import java.util.Optional;

public interface SensorService {
    List<Sensor> getSensorsByDeviceId(Long deviceId);
    Optional<Sensor> getSensorById(Long id);
    Sensor createSensor(Sensor sensor);
    void deleteSensor(Long id);
}
