package com.ssafy.shieldron.controller;

import com.ssafy.shieldron.dto.request.BatteryUpdateRequest;
import com.ssafy.shieldron.dto.request.DroneMatchRequest;
import com.ssafy.shieldron.dto.request.DroneAssignmentRequest;
import com.ssafy.shieldron.dto.request.DroneCancelRequest;
import com.ssafy.shieldron.dto.response.DroneAssignmentResponse;
import com.ssafy.shieldron.dto.response.DroneMatchResponse;
import com.ssafy.shieldron.global.CurrentUser;
import com.ssafy.shieldron.service.DroneService;
import jakarta.validation.Valid;
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

    @PostMapping("/routes")
    public ResponseEntity<?> droneAssignment(@Valid @RequestBody DroneAssignmentRequest droneAssignmentRequest,
                                             @CurrentUser String phoneNumber) {
        DroneAssignmentResponse droneAssignmentResponse = droneService.droneAssignment(droneAssignmentRequest, phoneNumber);
        return ResponseEntity.ok().body(droneAssignmentResponse);
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> droneCancel(@Valid @RequestBody DroneCancelRequest droneCancelRequest,
                                         @CurrentUser String phoneNumber) {
        droneService.droneCancel(droneCancelRequest, phoneNumber);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/match")
    public ResponseEntity<?> droneMatch(@Valid @RequestBody DroneMatchRequest droneMatchRequest,
                                        @CurrentUser String phoneNumber) {
        DroneMatchResponse droneMatchResponse = droneService.droneMatch(droneMatchRequest, phoneNumber);
        return ResponseEntity.ok().body(droneMatchResponse);
    }

    @PostMapping("/end")
    public ResponseEntity<?> droneEnd(@Valid @RequestBody DroneCancelRequest droneCancelRequest,
                                      @CurrentUser String phoneNumber) {
        droneService.droneEnd(droneCancelRequest, phoneNumber);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/battery")
    public ResponseEntity<?> batteryUpdate(@Valid @RequestBody BatteryUpdateRequest batteryUpdateRequest) {
        droneService.batteryUpdate(batteryUpdateRequest);
        return ResponseEntity.ok().build();
    }
}
