package com.ssafy.shieldron.controller;

import com.ssafy.shieldron.dto.request.GetHivesInfoRequest;
import com.ssafy.shieldron.dto.request.KeywordRequest;
import com.ssafy.shieldron.dto.response.HiveResponse;
import com.ssafy.shieldron.dto.response.HiveSearchResponse;
import com.ssafy.shieldron.service.HiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hives")
public class HiveController {

    private final HiveService hiveService;

    // TODO 유효성 검증
    @GetMapping
    public ResponseEntity<?> getHivesInfoByGPS(@RequestBody GetHivesInfoRequest getHivesInfoRequest) {
        List<HiveResponse> hivesInfo = hiveService.getHivesInfo(getHivesInfoRequest);
        return ResponseEntity.ok().body(hivesInfo);
    }

    @GetMapping("/search")
    public ResponseEntity<?> getHivesInfoByKeyword(@RequestBody KeywordRequest keywordRequest) {
        List<HiveSearchResponse> hivesInfo = hiveService.getHivesInfoByKeyword(keywordRequest);
        return ResponseEntity.ok().body(hivesInfo);
    }
}
