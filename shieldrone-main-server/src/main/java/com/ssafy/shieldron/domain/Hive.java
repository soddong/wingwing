package com.ssafy.shieldron.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hive extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToMany(mappedBy = "hive", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Drone> drones = new ArrayList<>();

    @Column(name = "hive_name", nullable = false)
    private String hiveName;

    @Column(name = "hive_no", nullable = false)
    private Integer hiveNo;

    @Column(name = "direction", nullable = false)
    private String direction;

    @Column(name = "hive_lat", precision = 9, scale = 6, nullable = false)
    private BigDecimal hiveLat;

    @Column(name = "hive_lng", precision = 9, scale = 6, nullable = false)
    private BigDecimal hiveLng;

    @Column(name = "hive_ip", length = 15, nullable = false)
    private String hiveIp;

    public void addDrone(Drone drone) {
        drones.add(drone);
        drone.assignHive(this);
    }

}
