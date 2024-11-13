package com.ssafy.shieldron.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DroneUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "drone_id", nullable = false)
    private Drone drone;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @Builder
    public DroneUser(Drone drone, User user, BigDecimal startLat, BigDecimal startLng,
                     BigDecimal endLat, BigDecimal endLng) {
        this.drone = drone;
        this.user = user;
    }

}
