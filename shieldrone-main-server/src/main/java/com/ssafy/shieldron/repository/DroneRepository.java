package com.ssafy.shieldron.repository;

import com.ssafy.shieldron.domain.Drone;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DroneRepository extends JpaRepository<Drone, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT d FROM Drone d
    WHERE d.status = (뭐넣을지 모름)
    AND d.battery >= :requiredBattery
    ORDER BY d.battery DESC
""")
    Optional<Drone> findFirstAvailableDroneWithLock(@Param("requiredBattery") Integer requiredBattery);

    Optional<Drone> findByDroneCode(Integer droneCode);
}
