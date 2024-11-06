package com.ssafy.shieldron.dto.request;

public record DroneAssignmentRequest(
        LocationRequest startLocation,
        LocationRequest endLocation
) {
}
