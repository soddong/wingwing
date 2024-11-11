package com.ssafy.shieldron.service;

import com.ssafy.shieldron.domain.Drone;
import com.ssafy.shieldron.domain.DroneUser;
import com.ssafy.shieldron.domain.DroneUserStatus;
import com.ssafy.shieldron.domain.Hive;
import com.ssafy.shieldron.domain.User;
import com.ssafy.shieldron.dto.request.DroneAssignmentRequest;
import com.ssafy.shieldron.dto.request.DroneCancelRequest;
import com.ssafy.shieldron.dto.request.DroneMatchRequest;
import com.ssafy.shieldron.dto.request.LocationRequest;
import com.ssafy.shieldron.dto.response.DroneAssignmentResponse;
import com.ssafy.shieldron.dto.response.DroneMatchResponse;
import com.ssafy.shieldron.global.exception.CustomException;
import com.ssafy.shieldron.repository.DroneRepository;
import com.ssafy.shieldron.repository.DroneUserRepository;
import com.ssafy.shieldron.repository.HiveRepository;
import com.ssafy.shieldron.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import static com.ssafy.shieldron.global.exception.ErrorCode.ALREADY_MATCHED_DRONE;
import static com.ssafy.shieldron.global.exception.ErrorCode.ALREADY_MATCHED_HIVE;
import static com.ssafy.shieldron.global.exception.ErrorCode.DRONE_NOT_AVAILABLE;
import static com.ssafy.shieldron.global.exception.ErrorCode.INVALID_DRONE;
import static com.ssafy.shieldron.global.exception.ErrorCode.INVALID_HIVE;
import static com.ssafy.shieldron.global.exception.ErrorCode.INVALID_USER;
import static com.ssafy.shieldron.global.exception.ErrorCode.SAME_START_AND_END_LOCATION;
import static com.ssafy.shieldron.global.exception.ErrorCode.USER_ALREADY_HAS_DRONE;

@Service
@RequiredArgsConstructor
public class DroneService {
    private static final double BATTERY_PER_METER = 0.1; // 미터 당 소모하는 배터리 퍼센트
    private static final double SPEED_METERS_PER_MINUTE = 400.0; // 분당 이동하는 미터
    private static final double MINIMUM_REQUIRED_BATTERY = 20.0; // 최소 배터리

    private final DroneRepository droneRepository;
    private final UserRepository userRepository;
    private final DroneUserRepository droneUserRepository;
    private final HiveRepository hiveRepository;

    @Transactional
    public DroneAssignmentResponse droneAssignment(DroneAssignmentRequest droneAssignmentRequest, String phoneNumber) {
        Integer hiveId = droneAssignmentRequest.startLocation().hiveId();

        User user = getUserOrThrow(phoneNumber);
        Hive hive = getHiveOrThrow(hiveId);

        Optional<DroneUser> existingDroneUser = droneUserRepository.findByUser(user);
        if (existingDroneUser.isPresent()) {
            throw new CustomException(USER_ALREADY_HAS_DRONE);
        }

        if (hive.getHiveLat().equals(droneAssignmentRequest.endLocation().lat()) &&
                hive.getHiveLng().equals(droneAssignmentRequest.endLocation().lng())) {
            throw new CustomException(SAME_START_AND_END_LOCATION);
        }


        double distanceInMeters = calculateDistanceBetweenHiveAndEndLocation(hive, droneAssignmentRequest);
        Integer requiredBattery = calculateRequiredBattery(distanceInMeters);

        Drone drone = findAvailableDrone(requiredBattery);
        drone.updateActive(true);

        createDroneUser(drone, user, hive, droneAssignmentRequest.endLocation());

        return createAssignmentResponse(drone, distanceInMeters);
    }

    @Transactional
    public void droneCancel(DroneCancelRequest droneCancelRequest, String phoneNumber) {
        Integer droneId = droneCancelRequest.droneId();
        User user = getUserOrThrow(phoneNumber);
        Drone drone = getDroneOrThrow(droneId);

        drone.updateActive(false);

        DroneUser droneUser = droneUserRepository.findByUserAndDrone(user, drone)
                .orElseThrow(() -> new CustomException(INVALID_DRONE));

        droneUserRepository.delete(droneUser);
    }

    @Transactional
    public DroneMatchResponse droneMatch(DroneMatchRequest droneMatchRequest, String phoneNumber) {
        Integer droneId = droneMatchRequest.droneId();
        Integer droneCode = droneMatchRequest.droneCode();
        User user = getUserOrThrow(phoneNumber);
        Drone drone = getDroneOrThrow(droneId);

        DroneUser droneUser = droneUserRepository.findByUserAndDrone(user, drone)
                .orElseThrow(() -> new CustomException(INVALID_DRONE));
        if (droneUser.getStatus() != DroneUserStatus.TEMPORARY) {
            throw new CustomException(ALREADY_MATCHED_DRONE);
        }

        droneUser.updateStatus(DroneUserStatus.TEMPORARY);

        Hive hive = drone.getHive();
        if (hive == null) {
            throw new CustomException(INVALID_HIVE);
        }

        if (!Objects.equals(drone.getDroneCode(), droneCode)) {
            throw new CustomException(INVALID_DRONE);
        }

        String hiveIp = hive.getHiveIp();
        return new DroneMatchResponse(droneId, hiveIp);
    }

    private DroneAssignmentResponse createAssignmentResponse(Drone drone, double distanceInMeters) {
        int estimatedMinutes = (int) Math.ceil(distanceInMeters / SPEED_METERS_PER_MINUTE);
        String formattedDistance = String.format("%.0fm", distanceInMeters);

        return new DroneAssignmentResponse(
                drone.getId(),
                estimatedMinutes,
                formattedDistance
        );
    }

    private void createDroneUser(Drone drone, User user, Hive startHive, LocationRequest endLocation) {

        DroneUser droneUser = DroneUser.builder()
                .drone(drone)
                .user(user)
                .startLat(startHive.getHiveLat())
                .startLng(startHive.getHiveLng())
                .endLat(endLocation.lat())
                .endLng(endLocation.lng())
                .build();

        droneUserRepository.save(droneUser);
    }

    private double calculateDistanceBetweenHiveAndEndLocation(Hive hive, DroneAssignmentRequest droneAssignmentRequest) {
        return calculateDistance(
                hive.getHiveLat(), hive.getHiveLng(),
                droneAssignmentRequest.endLocation().lat(),
                droneAssignmentRequest.endLocation().lng()
        );
    }

    private double calculateDistance(BigDecimal startLat, BigDecimal startLng,
                                     BigDecimal endLat, BigDecimal endLng) {
        double lat1 = startLat.doubleValue();
        double lng1 = startLng.doubleValue();
        double lat2 = endLat.doubleValue();
        double lng2 = endLng.doubleValue();

        double earthRadius = 6371000;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return earthRadius * c;
    }

    private Integer calculateRequiredBattery(double distanceInMeters) {
        double battery = (distanceInMeters * BATTERY_PER_METER) + MINIMUM_REQUIRED_BATTERY;
        return (int) Math.ceil(battery);
    }

    private Drone findAvailableDrone(Integer requiredBattery) {
        return droneRepository.findFirstAvailableDroneWithLock(requiredBattery)
                .orElseThrow(() -> new CustomException(DRONE_NOT_AVAILABLE));
    }

    private Hive getHiveOrThrow(Integer hiveId) {
        Hive hive = hiveRepository.findById(hiveId)
                .orElseThrow(() -> new CustomException(INVALID_HIVE));

        if (hive.getDrone().isActive()) {
            throw new CustomException(ALREADY_MATCHED_HIVE);
        }

        return hive;
    }

    private Drone getDroneOrThrow(Integer droneId) {
        return droneRepository.findById(droneId)
                .orElseThrow(() -> new CustomException(INVALID_DRONE));
    }


    private User getUserOrThrow(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new CustomException(INVALID_USER));
    }

}
