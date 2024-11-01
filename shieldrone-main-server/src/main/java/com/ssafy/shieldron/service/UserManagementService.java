package com.ssafy.shieldron.service;

import com.ssafy.shieldron.domain.SmsAuth;
import com.ssafy.shieldron.dto.request.AuthCodeVerifyRequest;
import com.ssafy.shieldron.dto.request.SmsAuthRequest;
import com.ssafy.shieldron.dto.response.AuthCodeVerifyResponse;
import com.ssafy.shieldron.repository.SmsAuthRepository;
import com.ssafy.shieldron.util.SmsAuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserManagementService {

    private final SmsAuthUtil smsAuthUtil;
    private final SmsAuthRepository smsAuthRepository;

    @Transactional
    public void sendSmsAuth(SmsAuthRequest smsAuthRequest) throws Exception {
        String phoneNumber = smsAuthRequest.phoneNumber();

        softDeleteExistingSmsAuth(phoneNumber);
        sendSmsAndSaveSmsAuth(phoneNumber);
    }

    public void verifyAuthCode(AuthCodeVerifyRequest authCodeVerifyRequest) {
        String phoneNumber = authCodeVerifyRequest.phoneNumber();
        String authCode = authCodeVerifyRequest.authCode();

        Optional<SmsAuth> existingSmsAuth = smsAuthRepository.findByPhoneNumber(phoneNumber);
        SmsAuth smsAuth = existingSmsAuth.get();
        if (!smsAuth.getAuthCode().equals(authCode)) {
            // 코드가 틀렸음을 알린다.
        }
        // 코드가 맞았으면 기존의 회원인지 조회한다.
        // 기존의 회원이면 true, 아니면 false
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
