package com.ssafy.shieldron.controller;

import com.ssafy.shieldron.dto.request.EndPosRequest;
import com.ssafy.shieldron.dto.response.EndPosResponse;
import com.ssafy.shieldron.global.CurrentUser;
import com.ssafy.shieldron.service.UserSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settings")
public class UserSettingController {

    private final UserSettingService userSettingService;

    @PatchMapping("/end-pos")
    public ResponseEntity<?> updateEndPos(@RequestBody EndPosRequest endPosRequest,
                                          @CurrentUser String phoneNumber) {
        userSettingService.updateEndPos(endPosRequest, phoneNumber);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/end-pos")
    public ResponseEntity<?> getEndPos(@CurrentUser String phoneNumber) {
        EndPosResponse endPosResponse = userSettingService.getEndPos(phoneNumber);
        return ResponseEntity.ok().body(endPosResponse);
    }

}
