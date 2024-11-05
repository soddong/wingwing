package com.ssafy.shieldron.service;

import com.ssafy.shieldron.domain.SmsAuth;
import com.ssafy.shieldron.domain.User;
import com.ssafy.shieldron.dto.request.SignUpRequest;
import com.ssafy.shieldron.dto.request.AuthCodeVerifyRequest;
import com.ssafy.shieldron.dto.request.SmsAuthRequest;
import com.ssafy.shieldron.dto.response.CheckIsUserResponse;
import com.ssafy.shieldron.global.exception.CustomException;
import com.ssafy.shieldron.repository.SmsAuthRepository;
import com.ssafy.shieldron.repository.UserRepository;
import com.ssafy.shieldron.global.util.SmsAuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.ssafy.shieldron.global.exception.ErrorCode.*;

@Slf4j
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

    @Transactional
    public CheckIsUserResponse verifyAuthCode(AuthCodeVerifyRequest authCodeVerifyRequest) {
        String phoneNumber = authCodeVerifyRequest.phoneNumber();
        String authCode = authCodeVerifyRequest.authCode();

        validateSmsAuth(phoneNumber, authCode);

        return checkIsUser(phoneNumber);
    }

    @Transactional
    public void signUp(SignUpRequest signUpRequest) {
        String phoneNumber = signUpRequest.phoneNumber();
        
        checkSmsAuthIsVerified(phoneNumber);
        checkDuplicatedUser(phoneNumber);

        saveNewUser(signUpRequest, phoneNumber);
    }

    private void saveNewUser(SignUpRequest signUpRequest, String phoneNumber) {
        String username = signUpRequest.username();
        LocalDate birthday = signUpRequest.birthday();
        User user = User.builder()
                .phoneNumber(phoneNumber)
                .username(username)
                .birthday(birthday)
                .build();
        userRepository.save(user);
    }

    private void checkDuplicatedUser(String phoneNumber) {
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new CustomException(DUPLICATE_USER);
        }
    }

    private void checkSmsAuthIsVerified(String phoneNumber) {
        Optional<SmsAuth> existingSmsAuth = smsAuthRepository.findByPhoneNumber(phoneNumber);
        if (existingSmsAuth.isEmpty()) {
           throw new CustomException(SMS_AUTH_REQUIRED);
        }
        SmsAuth smsAuth = existingSmsAuth.get();
        if (!smsAuth.isVerified()) {
            throw new CustomException(SMS_AUTH_REQUIRED);
        }
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

        smsAuth.updateIsVerifiedToTrue();
    }

    private void sendSmsAndSaveSmsAuth(String phoneNumber) throws Exception {
        String authCode = smsAuthUtil.sendSms(phoneNumber);
        SmsAuth smsAuth = SmsAuth.builder()
                .phoneNumber(phoneNumber)
                .authCode(authCode)
                .expiresAt(LocalDateTime.now().plusMinutes(2))
                .isDeleted(false)
                .isVerified(false)
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
