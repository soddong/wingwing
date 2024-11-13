package com.ssafy.shieldron.dto.response;

import com.ssafy.shieldron.domain.Drone;
import com.ssafy.shieldron.domain.DroneStatus;

public record DroneResponse(
        Integer droneId,
        Integer battery,
        DroneStatus status,
        Integer droneCode
) {
    public static DroneResponse toResponse(Drone drone) {
        return new DroneResponse(
                drone.getId(),
                drone.getBattery(),
                drone.getStatus(),
                drone.getDroneCode()
        );
    }
}