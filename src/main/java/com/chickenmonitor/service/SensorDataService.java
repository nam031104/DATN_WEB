package com.chickenmonitor.service;

import com.chickenmonitor.entity.SensorData;

import java.util.List;

public interface SensorDataService {
    List<SensorData> getSensorDataBySensorId(Long sensorId);
    SensorData saveSensorData(SensorData sensorData);
}
