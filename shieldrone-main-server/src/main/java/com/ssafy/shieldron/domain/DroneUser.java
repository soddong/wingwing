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

    @Column(name = "start_lat", precision = 9, scale = 6)
    private BigDecimal startLat;

    @Column(name = "start_lng", precision = 9, scale = 6)
    private BigDecimal startLng;

    @Column(name = "end_lat", precision = 9, scale = 6)
    private BigDecimal endLat;

    @Column(name = "end_lng", precision = 9, scale = 6)
    private BigDecimal endLng;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DroneUserStatus status;

    @Builder
    public DroneUser(Drone drone, User user, BigDecimal startLat, BigDecimal startLng,
                     BigDecimal endLat, BigDecimal endLng) {
        this.drone = drone;
        this.user = user;
        this.startLat = startLat;
        this.startLng = startLng;
        this.endLat = endLat;
        this.endLng = endLng;
        this.status = DroneUserStatus.TEMPORARY;
    }

    public void updateStatus(DroneUserStatus droneUserStatus) {
        this.status = droneUserStatus;
    }
}
