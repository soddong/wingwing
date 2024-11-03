package com.ssafy.shieldron.repository;

import com.ssafy.shieldron.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("SELECT u FROM User u WHERE u.phoneNumber = :phoneNumber")
    Optional<User> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);
}
