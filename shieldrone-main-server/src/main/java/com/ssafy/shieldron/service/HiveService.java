package com.ssafy.shieldron.service;

import com.ssafy.shieldron.domain.Hive;
import com.ssafy.shieldron.dto.request.GetHivesInfoRequest;
import com.ssafy.shieldron.dto.request.KeywordRequest;
import com.ssafy.shieldron.dto.response.HiveResponse;
import com.ssafy.shieldron.dto.response.HiveSearchResponse;
import com.ssafy.shieldron.repository.HiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class HiveService {
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double MAX_DISTANCE_KM = 1.0;
    private static final int MAX_DISTANCE_METER = 1000;

    private final HiveRepository hiveRepository;


    @Transactional(readOnly = true)
    public List<HiveResponse> getHivesInfo(GetHivesInfoRequest getHivesInfoRequest) {
        BigDecimal lat = getHivesInfoRequest.lat();
        BigDecimal lng = getHivesInfoRequest.lng();

        double latDegree = MAX_DISTANCE_KM / EARTH_RADIUS_KM * (180.0 / Math.PI);
        double lngDegree = latDegree / Math.cos(lat.doubleValue() * Math.PI / 180.0);

        BigDecimal minLat = lat.subtract(BigDecimal.valueOf(latDegree));
        BigDecimal maxLat = lat.add(BigDecimal.valueOf(latDegree));
        BigDecimal minLng = lng.subtract(BigDecimal.valueOf(lngDegree));
        BigDecimal maxLng = lng.add(BigDecimal.valueOf(lngDegree));

        List<Hive> hiveInBox = hiveRepository.findHivesInBoundingBox(minLat, maxLat, minLng, maxLng);
        return hiveInBox.stream()
                .map(hive -> HiveResponse.toResponse(hive, calculateDistance(lat, lng, hive.getHiveLat(), hive.getHiveLng())))
                .filter(response -> response.distance() <= MAX_DISTANCE_METER)
                .sorted(Comparator.comparing(HiveResponse::distance))
                .collect(Collectors.toList());

    }

    @Transactional(readOnly = true)
    public List<HiveSearchResponse> getHivesInfoByKeyword(KeywordRequest keywordRequest) {
        String keyword = keywordRequest.keyword();

        return hiveRepository.searchByKeyword(keyword).stream()
                .map(hive -> HiveSearchResponse.toResponse(hive, hive.getDrones()))
                .collect(Collectors.toList());
    }

    private int calculateDistance(BigDecimal lat1, BigDecimal lng1,
                                  BigDecimal lat2, BigDecimal lng2) {
        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double dLat = lat2Rad - lat1Rad;
        double dLng = Math.toRadians(lng2.subtract(lng1).doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (int) (EARTH_RADIUS_KM * 1000 * c);
    }

}
