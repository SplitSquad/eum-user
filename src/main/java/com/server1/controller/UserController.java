package com.server1.controller;

import com.server1.dto.UserLangReq;
import com.server1.dto.UserPreferenceRes;
import com.server1.dto.UserReq;
import com.server1.dto.UserRes;
import com.server1.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/profile")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserRes> getProfile(
            @RequestHeader("X-User-Email") String email
    ) {
        return ResponseEntity.ok(userService.getProfile(email));
    }

    @PutMapping
    public ResponseEntity<UserRes> updateProfile(
            @RequestHeader("X-User-Email") String email,
            @Validated @RequestBody UserReq req
    ) {
        return ResponseEntity.ok(userService.updateProfile(email, req));
    }

    @PutMapping("/language")
    public ResponseEntity<UserPreferenceRes> updateLanguage(
            @RequestHeader("X-User-Email") String email,
            @RequestBody UserLangReq req
    ) {
        return ResponseEntity.ok(userService.updateLanguage(email, req.getLanguage()));
    }
}
