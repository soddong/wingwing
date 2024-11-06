package com.ssafy.shieldron.repository;

import com.ssafy.shieldron.domain.DroneUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DroneUserRepository extends JpaRepository<DroneUser, Integer> {
}
