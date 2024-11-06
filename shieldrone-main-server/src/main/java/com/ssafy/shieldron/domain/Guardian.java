package com.ssafy.shieldron.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Guardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "relation", nullable = false)
    private String relation;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Builder
    public Guardian(User user, String relation, String phoneNumber) {
        this.user = user;
        this.relation = relation;
        this.phoneNumber = phoneNumber;
    }

    public void updateGuardianInfo(String relation, String guardianPhoneNumber) {
        this.relation = relation;
        this.phoneNumber = guardianPhoneNumber;
    }
}
