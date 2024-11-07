package com.ssafy.shieldron.repository;

import com.ssafy.shieldron.domain.Drone;
import com.ssafy.shieldron.domain.DroneUser;
import com.ssafy.shieldron.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DroneUserRepository extends JpaRepository<DroneUser, Integer> {

    Optional<DroneUser> findByUserAndDrone(User user, Drone drone);

    Optional<DroneUser> findByUser(User user);
}
