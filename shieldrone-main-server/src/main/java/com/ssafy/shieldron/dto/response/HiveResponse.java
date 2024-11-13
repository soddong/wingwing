package com.ssafy.shieldron.dto.response;

import com.ssafy.shieldron.domain.Hive;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public record HiveResponse(
        Integer hiveId,
        String hiveName,
        Integer hiveNo,
        String direction,
        BigDecimal lat,
        BigDecimal lng,
        int distance,
        List<DroneResponse> drones
) {

    public static HiveResponse toResponse(Hive hive, int distance) {
        List<DroneResponse> droneResponses = hive.getDrones().stream()
                .map(DroneResponse::toResponse)
                .collect(Collectors.toList());

        return new HiveResponse(
                hive.getId(),
                hive.getHiveName(),
                hive.getHiveNo(),
                hive.getDirection(),
                hive.getHiveLat(),
                hive.getHiveLng(),
                distance,
                droneResponses
        );
    }
}
