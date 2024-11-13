package com.ssafy.shieldron.dto.request;

public record BatteryUpdateRequest(
        Integer droneId,
        Integer battery
) {
}
