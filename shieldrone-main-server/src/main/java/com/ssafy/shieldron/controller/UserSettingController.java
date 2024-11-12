package com.ssafy.shieldron.controller;

import com.ssafy.shieldron.dto.request.EmergencyRequest;
import com.ssafy.shieldron.dto.request.EndPosRequest;
import com.ssafy.shieldron.dto.request.GetHivesInfoRequest;
import com.ssafy.shieldron.dto.request.GuardianDeleteRequest;
import com.ssafy.shieldron.dto.request.GuardianRequest;
import com.ssafy.shieldron.dto.request.GuardianUpdateRequest;
import com.ssafy.shieldron.dto.response.EndPosResponse;
import com.ssafy.shieldron.dto.response.GuardianResponse;
import com.ssafy.shieldron.global.CurrentUser;
import com.ssafy.shieldron.service.UserSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import retrofit2.http.DELETE;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settings")
public class UserSettingController {

    private final UserSettingService userSettingService;

    @PatchMapping("/end-pos")
    public ResponseEntity<?> updateEndPos(@Valid @RequestBody EndPosRequest endPosRequest,
                                          @CurrentUser String phoneNumber) {
        userSettingService.updateEndPos(endPosRequest, phoneNumber);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/end-pos")
    public ResponseEntity<?> getEndPos(@Valid @RequestBody GetHivesInfoRequest getHivesInfoRequest,
                                       @CurrentUser String phoneNumber) {
        EndPosResponse endPosResponse = userSettingService.getEndPos(getHivesInfoRequest, phoneNumber);
        return ResponseEntity.ok().body(endPosResponse);
    }

    @DeleteMapping("/end-pos")
    public ResponseEntity<?> deleteEndPos(@CurrentUser String phoneNumber) {
        EndPosRequest endPosRequest = new EndPosRequest(null, null, null);
        userSettingService.updateEndPos(endPosRequest, phoneNumber);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/guardian")
    public ResponseEntity<?> createGuardian(@Valid @RequestBody GuardianRequest guardianRequest,
                                            @CurrentUser String phoneNumber) {
        userSettingService.createGuardian(guardianRequest, phoneNumber);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/guardian")
    public ResponseEntity<?> getGuardian(@CurrentUser String phoneNumber) {
        List<GuardianResponse> guardians = userSettingService.getGuardian(phoneNumber);
        return ResponseEntity.ok().body(guardians);
    }

    @PutMapping("/guardian")
    public ResponseEntity<?> updateGuardian(@Valid @RequestBody GuardianUpdateRequest guardianUpdateRequest,
                                            @CurrentUser String phoneNumber) {
        userSettingService.updateGuardian(guardianUpdateRequest, phoneNumber);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/guardian")
    public ResponseEntity<?> deleteGuardian(@Valid @RequestBody GuardianDeleteRequest guardianDeleteRequest,
                                            @CurrentUser String phoneNumber) {
        userSettingService.deleteGuardian(guardianDeleteRequest, phoneNumber);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/emergency")
    public ResponseEntity<?> sendEmergency(@Valid @RequestBody EmergencyRequest emergencyRequest,
                                           @CurrentUser String phoneNumber) {
        userSettingService.sendEmergency(emergencyRequest, phoneNumber);
        return ResponseEntity.ok().build();
    }

}
