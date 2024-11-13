package com.ssafy.shieldron.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Drone extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "battery", nullable = false)
    private Integer battery;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DroneStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hive_id")
    private Hive hive;

    @Column(name = "drone_code", nullable = false)
    private Integer droneCode;


    public void assignHive(Hive hive) {
        this.hive = hive;
    }

    public void updateStatus(DroneStatus droneStatus) {
        this.status = droneStatus;
    }

    public void updateBattery(Integer battery) {
        this.battery = battery;
    }
}
