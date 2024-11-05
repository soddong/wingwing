package com.ssafy.shieldron.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "birthday", nullable = false)
    private LocalDate birthday;

    @OneToOne
    @JoinColumn(name = "start_hive_id")
    private Hive startHive;

    @Column(name = "end_lat", precision = 9, scale = 6)
    private BigDecimal endLat;

    @Column(name = "end_lng", precision = 9, scale = 6)
    private BigDecimal endLng;

    @Column(name = "distance")
    private Integer distance;

    @Column(name = "username")
    private String username;

    @Column(name = "detail_address")
    private String detailAddress;

    @Builder
    public User(String phoneNumber, LocalDate birthday, String username) {
        this.phoneNumber = phoneNumber;
        this.birthday = birthday;
        this.username = username;
    }

}
