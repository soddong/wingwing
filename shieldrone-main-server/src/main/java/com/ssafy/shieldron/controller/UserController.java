package com.ssafy.shieldron.controller;

import com.ssafy.shieldron.dto.SignUpRequest;
import com.ssafy.shieldron.dto.request.AuthCodeVerifyRequest;
import com.ssafy.shieldron.dto.request.SmsAuthRequest;
import com.ssafy.shieldron.dto.response.CheckIsUserResponse;
import com.ssafy.shieldron.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserManagementService userManagementService;

    @PostMapping("/send")
    public ResponseEntity<?> sendSms(@Valid @RequestBody SmsAuthRequest authSmsRequest) throws Exception {
        // TODO 검증
        userManagementService.sendSmsAuth(authSmsRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyAuthCode(@RequestBody AuthCodeVerifyRequest authCodeVerifyRequest) {
        // TODO 검증
        CheckIsUserResponse checkIsUserResponse = userManagementService.verifyAuthCode(authCodeVerifyRequest);
        return ResponseEntity.ok().body(checkIsUserResponse);
    }

    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@RequestBody SignUpRequest signUpRequest) {
        // TODO 검증
        userManagementService.signUp(signUpRequest);
        return ResponseEntity.ok().build();
    }
}
