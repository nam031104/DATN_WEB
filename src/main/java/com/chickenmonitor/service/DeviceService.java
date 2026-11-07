package com.chickenmonitor.service;

import com.chickenmonitor.entity.Device;

import java.util.List;
import java.util.Optional;

public interface DeviceService {
    List<Device> getAllDevices();
    List<Device> getDevicesByFarmId(Long farmId);
    Optional<Device> getDeviceById(Long id);
    Device createDevice(Device device);
    Device updateDevice(Long id, Device deviceDetails);
    void deleteDevice(Long id);
}
