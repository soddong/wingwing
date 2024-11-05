package com.ssafy.shieldron.service;

import com.ssafy.shieldron.domain.User;
import com.ssafy.shieldron.dto.request.EndPosRequest;
import com.ssafy.shieldron.dto.response.EndPosResponse;
import com.ssafy.shieldron.global.exception.CustomException;
import com.ssafy.shieldron.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static com.ssafy.shieldron.global.exception.ErrorCode.INVALID_USER;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingService {

    private final UserRepository userRepository;

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



    private User getUserOrThrow(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new CustomException(INVALID_USER));
    }

}
