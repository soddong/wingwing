package com.ssafy.shieldron.service;

import com.ssafy.shieldron.domain.SmsAuth;
import com.ssafy.shieldron.domain.User;
import com.ssafy.shieldron.dto.request.SignInRequest;
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
        // TODO 유효성 검증 코드 리팩토링 필요, Validator를 통해 controller에서 공통적으로 검증할것.
        Optional<SmsAuth> existingSmsAuth = smsAuthRepository.findByPhoneNumber(phoneNumber);
        if (existingSmsAuth.isEmpty()) {
            throw new CustomException(SMS_AUTH_REQUIRED);
        }
        SmsAuth smsAuth = existingSmsAuth.get();
        if (!smsAuth.isVerified()) {
            throw new CustomException(SMS_AUTH_REQUIRED);
        }
        Optional<User> existingUser = userRepository.findByPhoneNumber(phoneNumber);
        if (existingUser.isEmpty()) {
            throw new CustomException(INVALID_USER);
        }
        User user = existingUser.get();
        String username = user.getUsername();
        String accessToken = jwtUtil.generateAccessToken(username, phoneNumber);
        String refreshToken = jwtUtil.generateRefreshToken(username, phoneNumber);

        return new SignInResponse(accessToken, refreshToken);
    }
}
