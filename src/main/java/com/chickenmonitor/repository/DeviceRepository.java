package com.chickenmonitor.repository;

import com.chickenmonitor.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    // Lấy danh sách thiết bị thuộc về một trang trại cụ thể
    List<Device> findByFarmFarmId(Long farmId);

    Optional<Device> findByDeviceIdAndFarmUserUserId(Long deviceId, Long userId);

    Optional<Device> findByDeviceCode(String deviceCode);

    boolean existsByDeviceCode(String deviceCode);

    Optional<Device> findByDeviceCodeAndFarmIsNull(String deviceCode);

    @Query("""
select d from Device d
    join fetch d.farm f
    join fetch f.user
    where d.deviceCode = :code
""")
    Optional<Device> findByDeviceCodeWithOwner(@Param("code") String code);
}