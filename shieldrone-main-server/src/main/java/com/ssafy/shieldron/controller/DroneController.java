package com.ssafy.shieldron.controller;

import com.ssafy.shieldron.dto.request.DroneAssignmentRequest;
import com.ssafy.shieldron.dto.response.DroneAssignmentResponse;
import com.ssafy.shieldron.global.CurrentUser;
import com.ssafy.shieldron.service.DroneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/drones")
public class DroneController {

    private final DroneService droneService;

    // TODO 유효성 검증
    @PostMapping("/routes")
    public ResponseEntity<?> droneAssignment(@RequestBody DroneAssignmentRequest droneAssignmentRequest,
                                             @CurrentUser String phoneNumber) {
        DroneAssignmentResponse droneAssignmentResponse = droneService.droneAssignment(droneAssignmentRequest, phoneNumber);
        return ResponseEntity.ok().body(droneAssignmentResponse);
    }

}
