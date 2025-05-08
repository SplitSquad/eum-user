package com.server1.controller;

import com.server1.dto.UserPreferenceReq;
import com.server1.dto.UserPreferenceRes;
import com.server1.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/preference")
@RequiredArgsConstructor
public class UserPreferenceController {
    private final UserPreferenceService prefService;

    @GetMapping
    public ResponseEntity<UserPreferenceRes> getPreference(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(prefService.getPreference(token));
    }

    @PostMapping
    public ResponseEntity<UserPreferenceRes> saveOrUpdate(
            @RequestHeader("Authorization") String token,
            @Validated @RequestBody UserPreferenceReq req
    ) {
        return ResponseEntity.ok(prefService.saveOrUpdate(token, req));
    }
}
