package com.chickenmonitor.service.impl;

import com.chickenmonitor.entity.SensorData;
import com.chickenmonitor.repository.SensorDataRepository;
import com.chickenmonitor.service.SensorDataService;
import com.chickenmonitor.service.SensorAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SensorDataServiceImpl implements SensorDataService {

    private final SensorDataRepository sensorDataRepository;
    private final SensorAlertService sensorAlertService;

    @Override
    public List<SensorData> getSensorDataBySensorId(Long sensorId) {
        return sensorDataRepository.findBySensorSensorIdOrderByCreatedAtDesc(sensorId);
    }

    @Override
    public SensorData saveSensorData(SensorData sensorData) {
        SensorData saved = sensorDataRepository.save(sensorData);
        sensorAlertService.check(saved.getSensor(), saved.getValue(), saved.getUnit());
        return saved;
    }
}
