package com.chickenmonitor.service;

import com.chickenmonitor.entity.Farm;

import java.util.List;
import java.util.Optional;

public interface FarmService {
    List<Farm> getAllFarms();
    List<Farm> getFarmsByUserId(Long userId);
    Optional<Farm> getFarmById(Long id);
    Farm createFarm(Farm farm);
    Farm updateFarm(Long id, Farm farmDetails);
    void deleteFarm(Long id);
}
