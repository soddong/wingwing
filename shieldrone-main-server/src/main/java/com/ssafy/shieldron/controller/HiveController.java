package com.ssafy.shieldron.controller;

import com.ssafy.shieldron.dto.request.GetHivesInfoRequest;
import com.ssafy.shieldron.dto.request.KeywordRequest;
import com.ssafy.shieldron.dto.response.HiveResponse;
import com.ssafy.shieldron.dto.response.HiveSearchResponse;
import com.ssafy.shieldron.service.HiveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hives")
public class HiveController {

    private final HiveService hiveService;

    @PostMapping
    public ResponseEntity<?> getHivesInfoByGPS(@Valid @RequestBody GetHivesInfoRequest getHivesInfoRequest) {
        List<HiveResponse> hivesInfo = hiveService.getHivesInfo(getHivesInfoRequest);
        return ResponseEntity.ok().body(hivesInfo);
    }

    @PostMapping("/search")
    public ResponseEntity<?> getHivesInfoByKeyword(@Valid @RequestBody KeywordRequest keywordRequest) {
        List<HiveSearchResponse> hivesInfo = hiveService.getHivesInfoByKeyword(keywordRequest);
        return ResponseEntity.ok().body(hivesInfo);
    }
}
