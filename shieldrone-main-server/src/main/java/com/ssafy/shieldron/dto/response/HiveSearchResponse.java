package com.ssafy.shieldron.dto.response;

import com.ssafy.shieldron.domain.Hive;

import java.math.BigDecimal;

public record HiveSearchResponse(
        Integer hiveId,
        String hiveName,
        Integer hiveNo,
        String direction,
        BigDecimal lat,
        BigDecimal lng
) {
    public static HiveSearchResponse toResponse(Hive hive) {
        return new HiveSearchResponse(
                hive.getId(),
                hive.getHiveName(),
                hive.getHiveNo(),
                hive.getDirection(),
                hive.getHiveLat(),
                hive.getHiveLng()
        );
    }
}
