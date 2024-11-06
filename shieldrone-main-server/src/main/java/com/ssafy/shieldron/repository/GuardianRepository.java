package com.ssafy.shieldron.repository;

import com.ssafy.shieldron.domain.Guardian;
import com.ssafy.shieldron.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuardianRepository extends JpaRepository<Guardian, Integer> {

    Optional<Guardian> findByUserAndPhoneNumber(User user, String phoneNumber);
    int countByUser(User user);

    List<Guardian> findAllByUser(User user);

    Optional<Guardian> findByIdAndUser(Integer guardianId, User user);
}
