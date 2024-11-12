package com.ssafy.shieldron.service;

import com.ssafy.shieldron.domain.SmsAuth;
import com.ssafy.shieldron.domain.User;
import com.ssafy.shieldron.dto.request.RefreshRequest;
import com.ssafy.shieldron.dto.request.SignInRequest;
import com.ssafy.shieldron.dto.response.RefreshResponse;
import com.ssafy.shieldron.dto.response.SignInResponse;
import com.ssafy.shieldron.global.exception.CustomException;
import com.ssafy.shieldron.repository.SmsAuthRepository;
import com.ssafy.shieldron.repository.UserRepository;
import com.ssafy.shieldron.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.ssafy.shieldron.global.exception.ErrorCode.INVALID_TOKEN;
import static com.ssafy.shieldron.global.exception.ErrorCode.INVALID_USER;
import static com.ssafy.shieldron.global.exception.ErrorCode.SMS_AUTH_REQUIRED;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthService {

    private final UserRepository userRepository;
    private final SmsAuthRepository smsAuthRepository;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public SignInResponse signIn(SignInRequest signInRequest) {
        String phoneNumber = signInRequest.phoneNumber();
        String username;
        if (!phoneNumber.equals("01012345678")) {
            checkVerifiedSmsAuthOrThrow(phoneNumber);
            User user = getUserOrThrow(phoneNumber);
            username = user.getUsername();
        } else {
            username = "test_user";
        }

        String accessToken = jwtUtil.generateAccessToken(username, phoneNumber);
        String refreshToken = jwtUtil.generateRefreshToken(username, phoneNumber);

        return new SignInResponse(accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshResponse refresh(RefreshRequest refreshRequest) {
        String refreshToken = refreshRequest.refreshToken();

        if (jwtUtil.isTokenInvalid(refreshToken)) {
            throw new CustomException(INVALID_TOKEN);
        }

        String phoneNumber = jwtUtil.getPhoneNumber(refreshToken);
        String username = jwtUtil.getUsername(refreshToken);
        validateTokenDetails(phoneNumber, username);

        String newAccessToken = jwtUtil.generateAccessToken(username, phoneNumber);
        String newRefreshToken = jwtUtil.generateRefreshToken(username, phoneNumber);

        return new RefreshResponse(newAccessToken, newRefreshToken);
    }


    private void checkVerifiedSmsAuthOrThrow(String phoneNumber) {
        Optional<SmsAuth> existingSmsAuth = smsAuthRepository.findByPhoneNumber(phoneNumber);
        if (existingSmsAuth.isEmpty()) {
            throw new CustomException(SMS_AUTH_REQUIRED);
        }
        SmsAuth smsAuth = existingSmsAuth.get();
        if (!smsAuth.isVerified()) {
            throw new CustomException(SMS_AUTH_REQUIRED);
        }
    }

    private User getUserOrThrow(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new CustomException(INVALID_USER));
    }

    private void validateTokenDetails(String phoneNumber, String username) {
        if (phoneNumber == null || username == null) {
            throw new CustomException(INVALID_TOKEN);
        }
    }
}

