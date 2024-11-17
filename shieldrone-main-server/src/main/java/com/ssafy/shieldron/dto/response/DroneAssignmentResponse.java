package com.ssafy.shieldron.dto.response;

public record DroneAssignmentResponse(
        Integer droneId,
        Integer estimatedTime,
        Integer distance
) {
}
