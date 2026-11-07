package com.chickenmonitor.service.impl;

import com.chickenmonitor.entity.SensorData;
import com.chickenmonitor.repository.SensorDataRepository;
import com.chickenmonitor.service.SensorDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SensorDataServiceImpl implements SensorDataService {

    private final SensorDataRepository sensorDataRepository;

    @Override
    public List<SensorData> getSensorDataBySensorId(Long sensorId) {
        return sensorDataRepository.findBySensorSensorIdOrderByCreatedAtDesc(sensorId);
    }

    @Override
    public SensorData saveSensorData(SensorData sensorData) {
        return sensorDataRepository.save(sensorData);
    }
}
