package com.ssafy.shieldron.service;

import com.ssafy.shieldron.domain.Guardian;
import com.ssafy.shieldron.domain.User;
import com.ssafy.shieldron.dto.request.EmergencyRequest;
import com.ssafy.shieldron.dto.request.EndPosRequest;
import com.ssafy.shieldron.dto.request.GetHivesInfoRequest;
import com.ssafy.shieldron.dto.request.GuardianDeleteRequest;
import com.ssafy.shieldron.dto.request.GuardianRequest;
import com.ssafy.shieldron.dto.request.GuardianUpdateRequest;
import com.ssafy.shieldron.dto.response.EndPosResponse;
import com.ssafy.shieldron.dto.response.GuardianResponse;
import com.ssafy.shieldron.global.exception.CustomException;
import com.ssafy.shieldron.global.util.SmsAuthUtil;
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
import static com.ssafy.shieldron.global.exception.ErrorCode.NO_GUARDIAN_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private final UserRepository userRepository;
    private final GuardianRepository guardianRepository;
    private final SmsAuthUtil smsAuthUtil;

    @Transactional
    public void updateEndPos(EndPosRequest endPosRequest, String phoneNumber) {
        String detail = endPosRequest.detail();
        BigDecimal lat = endPosRequest.lat();
        BigDecimal lng = endPosRequest.lng();

        User user = getUserOrThrow(phoneNumber);

        user.updateEndPos(detail, lat, lng);
    }

    @Transactional(readOnly = true)
    public EndPosResponse getEndPos(GetHivesInfoRequest getHivesInfoRequest, String phoneNumber) {
        BigDecimal lat = getHivesInfoRequest.lat();
        BigDecimal lng = getHivesInfoRequest.lng();
        User user = getUserOrThrow(phoneNumber);
        String detailAddress = user.getDetailAddress();
        BigDecimal endLat = user.getEndLat();
        BigDecimal endLng = user.getEndLng();
        int distance = calculateDistance(lat, lng, endLat, endLat);
        return new EndPosResponse(detailAddress, endLat, endLng, distance);
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

    @Transactional
    public void sendEmergency(EmergencyRequest emergencyRequest, String phoneNumber) {
        User user = getUserOrThrow(phoneNumber);
        BigDecimal lat = emergencyRequest.lat();
        BigDecimal lng = emergencyRequest.lng();
        String username = user.getUsername();

        List<Guardian> guardians = guardianRepository.findAllByUser(user);

        if (guardians.isEmpty()) {
            log.error("사용자 {} (전화번호: {})에게 등록된 보호자가 없습니다.", username, phoneNumber);
            throw new CustomException(NO_GUARDIAN_FOUND);
        }

        for (Guardian guardian : guardians) {
            try {
                smsAuthUtil.sendEmergency(guardian.getPhoneNumber(), lat, lng, username);
            } catch (Exception e) {
                log.error("보호자 {} (전화번호: {})에게 긴급 메시지 전송 실패", guardian.getRelation(), guardian.getPhoneNumber(), e);
            }
        }
    }

    private User getUserOrThrow(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new CustomException(INVALID_USER));
    }
    private int calculateDistance(BigDecimal lat1, BigDecimal lng1,
                                  BigDecimal lat2, BigDecimal lng2) {
        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double dLat = lat2Rad - lat1Rad;
        double dLng = Math.toRadians(lng2.subtract(lng1).doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (int) (EARTH_RADIUS_KM * 1000 * c);
    }


}
