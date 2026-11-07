package com.chickenmonitor.repository;

import com.chickenmonitor.entity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FarmRepository extends JpaRepository<Farm, Long> {
    // Lấy danh sách các trang trại thuộc về một user cụ thể
    List<Farm> findByUserUserId(Long userId);

    java.util.Optional<Farm> findByFarmIdAndUserUserId(Long farmId, Long userId);

    java.util.Optional<Farm> findByFarmIdAndUserUserIdAndUserUsername(Long farmId, Long userId, String username);
}