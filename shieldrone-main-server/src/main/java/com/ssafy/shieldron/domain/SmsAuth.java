package com.ssafy.shieldron.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SmsAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "auth_code", nullable = false)
    private String authCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    @Builder
    public SmsAuth(String phoneNumber, String authCode, LocalDateTime expiresAt, Boolean isDeleted, Boolean isVerified) {
        this.phoneNumber = phoneNumber;
        this.authCode = authCode;
        this.expiresAt = expiresAt;
        this.isDeleted = isDeleted;
        this.isVerified = isVerified;
    }

    public void softDelete() {
        this.isDeleted = true;
    }

    public void updateIsVerifiedToTrue() {
        this.isVerified = true;
    }
}
