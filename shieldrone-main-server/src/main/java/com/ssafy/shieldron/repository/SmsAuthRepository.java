package com.ssafy.shieldron.repository;

import com.ssafy.shieldron.domain.SmsAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SmsAuthRepository extends JpaRepository<SmsAuth, Integer> {

    @Query("SELECT s FROM SmsAuth s WHERE s.phoneNumber = :phoneNumber AND s.isDeleted = false")
    Optional<SmsAuth> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);
}
