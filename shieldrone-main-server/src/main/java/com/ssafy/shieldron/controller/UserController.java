package com.ssafy.shieldron.controller;

import com.ssafy.shieldron.dto.request.RefreshRequest;
import com.ssafy.shieldron.dto.request.SignInRequest;
import com.ssafy.shieldron.dto.request.SignUpRequest;
import com.ssafy.shieldron.dto.request.AuthCodeVerifyRequest;
import com.ssafy.shieldron.dto.request.SmsAuthRequest;
import com.ssafy.shieldron.dto.response.CheckIsUserResponse;
import com.ssafy.shieldron.dto.response.RefreshResponse;
import com.ssafy.shieldron.dto.response.SignInResponse;
import com.ssafy.shieldron.service.UserAuthService;
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
    private final UserAuthService userAuthService;

    @PostMapping("/send")
    public ResponseEntity<?> sendSms(@Valid @RequestBody SmsAuthRequest authSmsRequest) throws Exception {
        String phoneNumber = authSmsRequest.phoneNumber();
        if(!phoneNumber.equals("01012345678")) {
            userManagementService.sendSmsAuth(authSmsRequest);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyAuthCode(@Valid @RequestBody AuthCodeVerifyRequest authCodeVerifyRequest) {
        CheckIsUserResponse checkIsUserResponse = userManagementService.verifyAuthCode(authCodeVerifyRequest);
        return ResponseEntity.ok().body(checkIsUserResponse);
    }

    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        userManagementService.signUp(signUpRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@Valid @RequestBody SignInRequest signInRequest) {
        SignInResponse signInResponse = userAuthService.signIn(signInRequest);
        return ResponseEntity.ok().body(signInResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest refreshRequest) {
        RefreshResponse refreshResponse = userAuthService.refresh(refreshRequest);
        return ResponseEntity.ok().body(refreshResponse);
    }
}
