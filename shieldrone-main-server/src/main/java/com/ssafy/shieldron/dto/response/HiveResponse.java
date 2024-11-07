package com.ssafy.shieldron.dto.response;

import com.ssafy.shieldron.domain.Hive;

import java.math.BigDecimal;

public record HiveResponse(
        Integer hiveId,
        String hiveName,
        Integer hiveNo,
        String direction,
        BigDecimal lat,
        BigDecimal lng,
        double distance
) {

    public static HiveResponse toResponse(Hive hive, double distance) {
        return new HiveResponse(
                hive.getId(),
                hive.getHiveName(),
                hive.getHiveNo(),
                hive.getDirection(),
                hive.getHiveLat(),
                hive.getHiveLng(),
                distance
        );
    }
}