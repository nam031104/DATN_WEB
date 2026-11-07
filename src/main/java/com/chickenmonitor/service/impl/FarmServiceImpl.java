package com.chickenmonitor.service.impl;

import com.chickenmonitor.entity.Farm;
import com.chickenmonitor.repository.FarmRepository;
import com.chickenmonitor.service.FarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FarmServiceImpl implements FarmService {

    private final FarmRepository farmRepository;

    @Override
    public List<Farm> getAllFarms() {
        return farmRepository.findAll();
    }

    @Override
    public List<Farm> getFarmsByUserId(Long userId) {
        return farmRepository.findByUserUserId(userId);
    }

    @Override
    public Optional<Farm> getFarmById(Long id) {
        return farmRepository.findById(id);
    }

    @Override
    public Farm createFarm(Farm farm) {
        return farmRepository.save(farm);
    }

    @Override
    public Farm updateFarm(Long id, Farm farmDetails) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Farm với ID: " + id));

        farm.setName(farmDetails.getName());
        farm.setLocation(farmDetails.getLocation());
        farm.setDescription(farmDetails.getDescription());
        return farmRepository.save(farm);
    }

    @Override
    public void deleteFarm(Long id) {
        if (!farmRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy Farm với ID: " + id);
        }
        farmRepository.deleteById(id);
    }
}
