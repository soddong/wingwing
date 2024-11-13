package com.ssafy.shieldron.dto.response;

import com.ssafy.shieldron.domain.Drone;
import com.ssafy.shieldron.domain.Hive;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public record HiveSearchResponse(
        Integer hiveId,
        String hiveName,
        Integer hiveNo,
        String direction,
        BigDecimal lat,
        BigDecimal lng,
        List<DroneResponse> drones
) {
    public static HiveSearchResponse toResponse(Hive hive, List<Drone> drones) {
        List<DroneResponse> droneResponses = hive.getDrones().stream()
                .map(DroneResponse::toResponse)
                .collect(Collectors.toList());

        return new HiveSearchResponse(
                hive.getId(),
                hive.getHiveName(),
                hive.getHiveNo(),
                hive.getDirection(),
                hive.getHiveLat(),
                hive.getHiveLng(),
                droneResponses
        );
    }
}
