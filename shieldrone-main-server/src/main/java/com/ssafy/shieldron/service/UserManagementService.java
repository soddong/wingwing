package com.ssafy.shieldron.service;

import com.ssafy.shieldron.domain.SmsAuth;
import com.ssafy.shieldron.domain.User;
import com.ssafy.shieldron.dto.request.AuthCodeVerifyRequest;
import com.ssafy.shieldron.dto.request.SmsAuthRequest;
import com.ssafy.shieldron.dto.response.CheckIsUserResponse;
import com.ssafy.shieldron.exception.CustomException;
import com.ssafy.shieldron.exception.ErrorCode;
import com.ssafy.shieldron.repository.SmsAuthRepository;
import com.ssafy.shieldron.repository.UserRepository;
import com.ssafy.shieldron.util.SmsAuthUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.ssafy.shieldron.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final SmsAuthUtil smsAuthUtil;
    private final SmsAuthRepository smsAuthRepository;
    private final UserRepository userRepository;

    private static final CheckIsUserResponse USER_EXISTS_RESPONSE = new CheckIsUserResponse(true);
    private static final CheckIsUserResponse USER_NOT_EXISTS_RESPONSE = new CheckIsUserResponse(false);

    @Transactional
    public void sendSmsAuth(SmsAuthRequest smsAuthRequest) throws Exception {
        String phoneNumber = smsAuthRequest.phoneNumber();

        softDeleteExistingSmsAuth(phoneNumber);
        sendSmsAndSaveSmsAuth(phoneNumber);
    }

    @Transactional(readOnly = true)
    public CheckIsUserResponse verifyAuthCode(AuthCodeVerifyRequest authCodeVerifyRequest) {
        String phoneNumber = authCodeVerifyRequest.phoneNumber();
        String authCode = authCodeVerifyRequest.authCode();

        validateSmsAuth(phoneNumber, authCode);

        return checkIsUser(phoneNumber);
    }

    private CheckIsUserResponse checkIsUser(String phoneNumber) {
        Optional<User> existingUser = userRepository.findByPhoneNumber(phoneNumber);
        if (existingUser.isPresent()) {
            return USER_EXISTS_RESPONSE;
        }
        return USER_NOT_EXISTS_RESPONSE;
    }

    private void validateSmsAuth(String phoneNumber, String authCode) {
        Optional<SmsAuth> existingSmsAuth = smsAuthRepository.findByPhoneNumber(phoneNumber);

        if (existingSmsAuth.isEmpty()) {
            throw new CustomException(USER_NOT_FOUND);
        }
        SmsAuth smsAuth = existingSmsAuth.get();

        if (smsAuth.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CustomException(AUTH_CODE_EXPIRED);
        }
        if (!smsAuth.getAuthCode().equals(authCode)) {
            throw new CustomException(INVALID_AUTH_CODE);
        }
    }

    private void sendSmsAndSaveSmsAuth(String phoneNumber) throws Exception {
        String authCode = smsAuthUtil.sendSms(phoneNumber);
        SmsAuth smsAuth = SmsAuth.builder()
                .phoneNumber(phoneNumber)
                .authCode(authCode)
                .expiresAt(LocalDateTime.now().plusMinutes(2))
                .isDeleted(false)
                .build();
        smsAuthRepository.save(smsAuth);
    }

    private void softDeleteExistingSmsAuth(String phoneNumber) {
        Optional<SmsAuth> existingSmsAuth = smsAuthRepository.findByPhoneNumber(phoneNumber);

        if (existingSmsAuth.isPresent()) {
            SmsAuth smsAuthToDelete = existingSmsAuth.get();
            smsAuthToDelete.softDelete();
            smsAuthRepository.save(smsAuthToDelete);
        }
    }

}
