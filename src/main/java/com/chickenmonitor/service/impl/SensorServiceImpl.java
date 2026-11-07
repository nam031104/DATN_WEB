package com.chickenmonitor.service.impl;

import com.chickenmonitor.entity.Sensor;
import com.chickenmonitor.repository.SensorRepository;
import com.chickenmonitor.service.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SensorServiceImpl implements SensorService {

    private final SensorRepository sensorRepository;

    @Override
    public List<Sensor> getSensorsByDeviceId(Long deviceId) {
        return sensorRepository.findByDeviceDeviceId(deviceId);
    }

    @Override
    public Optional<Sensor> getSensorById(Long id) {
        return sensorRepository.findById(id);
    }

    @Override
    public Sensor createSensor(Sensor sensor) {
        return sensorRepository.save(sensor);
    }

    @Override
    public void deleteSensor(Long id) {
        if (!sensorRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy Sensor với ID: " + id);
        }
        sensorRepository.deleteById(id);
    }
}
