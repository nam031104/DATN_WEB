package com.chickenmonitor.service.impl;

import com.chickenmonitor.entity.Device;
import com.chickenmonitor.repository.DeviceRepository;
import com.chickenmonitor.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;

    @Override
    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    @Override
    public List<Device> getDevicesByFarmId(Long farmId) {
        return deviceRepository.findByFarmFarmId(farmId);
    }

    @Override
    public Optional<Device> getDeviceById(Long id) {
        return deviceRepository.findById(id);
    }

    @Override
    public Device createDevice(Device device) {
        return deviceRepository.save(device);
    }

    @Override
    public Device updateDevice(Long id, Device deviceDetails) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Device với ID: " + id));

        device.setName(deviceDetails.getName());
        device.setTypeDevice(deviceDetails.getTypeDevice());
        device.setNote(deviceDetails.getNote());
        return deviceRepository.save(device);
    }

    @Override
    public void deleteDevice(Long id) {
        if (!deviceRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy Device với ID: " + id);
        }
        deviceRepository.deleteById(id);
    }
}