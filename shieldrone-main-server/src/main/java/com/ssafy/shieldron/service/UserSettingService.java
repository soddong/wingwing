package com.ssafy.shieldron.service;

import com.ssafy.shieldron.domain.Guardian;
import com.ssafy.shieldron.domain.User;
import com.ssafy.shieldron.dto.request.EndPosRequest;
import com.ssafy.shieldron.dto.request.GuardianDeleteRequest;
import com.ssafy.shieldron.dto.request.GuardianRequest;
import com.ssafy.shieldron.dto.request.GuardianUpdateRequest;
import com.ssafy.shieldron.dto.response.EndPosResponse;
import com.ssafy.shieldron.dto.response.GuardianResponse;
import com.ssafy.shieldron.global.exception.CustomException;
import com.ssafy.shieldron.repository.GuardianRepository;
import com.ssafy.shieldron.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ssafy.shieldron.global.exception.ErrorCode.GUARDIAN_ALREADY_EXISTS;
import static com.ssafy.shieldron.global.exception.ErrorCode.INVALID_GUARDIAN;
import static com.ssafy.shieldron.global.exception.ErrorCode.INVALID_USER;
import static com.ssafy.shieldron.global.exception.ErrorCode.MAX_GUARDIAN_REACHED;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingService {

    private final UserRepository userRepository;
    private final GuardianRepository guardianRepository;

    @Transactional
    public void updateEndPos(EndPosRequest endPosRequest, String phoneNumber) {
        String detail = endPosRequest.detail();
        BigDecimal lat = endPosRequest.lat();
        BigDecimal lng = endPosRequest.lng();

        User user = getUserOrThrow(phoneNumber);

        user.updateEndPos(detail, lat, lng);
    }

    @Transactional(readOnly = true)
    public EndPosResponse getEndPos(String phoneNumber) {
        User user = getUserOrThrow(phoneNumber);
        String detailAddress = user.getDetailAddress();
        BigDecimal endLat = user.getEndLat();
        BigDecimal endLng = user.getEndLng();
        return new EndPosResponse(detailAddress, endLat, endLng);
    }

    @Transactional
    public void createGuardian(GuardianRequest guardianRequest, String phoneNumber) {
        String relation = guardianRequest.relation();
        String guardianPhoneNumber = guardianRequest.phoneNumber();

        User user = getUserOrThrow(phoneNumber);

        int guardianCnt = guardianRepository.countByUser(user);
        if (guardianCnt >= 3) {
            throw new CustomException(MAX_GUARDIAN_REACHED);
        }


        Guardian guardian = Guardian.builder()
                .user(user)
                .relation(relation)
                .phoneNumber(guardianPhoneNumber)
                .build();

        guardianRepository.save(guardian);
    }

    @Transactional(readOnly = true)
    public List<GuardianResponse> getGuardian(String phoneNumber) {
        User user = getUserOrThrow(phoneNumber);
        List<Guardian> guardians = guardianRepository.findAllByUser(user);
        return guardians.stream()
                .map(guardian -> new GuardianResponse(guardian.getId(), guardian.getRelation(), guardian.getPhoneNumber()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateGuardian(GuardianUpdateRequest guardianUpdateRequest, String phoneNumber) {
        User user = getUserOrThrow(phoneNumber);
        Integer guardianId = guardianUpdateRequest.id();
        String relation = guardianUpdateRequest.relation();
        String guardianPhoneNumber = guardianUpdateRequest.phoneNumber();

        Guardian guardian = guardianRepository.findByIdAndUser(guardianId, user)
                .orElseThrow(() -> new CustomException(INVALID_GUARDIAN));

        guardian.updateGuardianInfo(relation, guardianPhoneNumber);
    }

    @Transactional
    public void deleteGuardian(GuardianDeleteRequest guardianDeleteRequest, String phoneNumber) {
        Integer guardianId = guardianDeleteRequest.id();

        User user = getUserOrThrow(phoneNumber);

        Guardian guardian = guardianRepository.findByIdAndUser(guardianId, user)
                .orElseThrow(() -> new CustomException(INVALID_GUARDIAN));

        guardianRepository.delete(guardian);
    }

    private User getUserOrThrow(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new CustomException(INVALID_USER));
    }

}