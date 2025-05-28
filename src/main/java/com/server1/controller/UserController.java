package com.server1.controller;

import com.server1.dto.*;
import com.server1.service.AwsS3Service;
import com.server1.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users/profile")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AwsS3Service awsS3Service;

    @GetMapping
    public ResponseEntity<UserRes> getProfile(
            @RequestHeader("Authorization") String token
    ) {
        return ResponseEntity.ok(userService.getProfile(token));
    }

    @PutMapping
    public ResponseEntity<UserRes> updateProfile(
            @RequestHeader("Authorization") String token,
            @Validated @RequestBody UserReq req
    ) {
        return ResponseEntity.ok(userService.updateProfile(token, req));
    }

    @PutMapping("/language")
    public ResponseEntity<UserPreferenceRes> updateLanguage(
            @RequestHeader("Authorization") String token,
            @RequestBody UserLangReq req
    ) {
        return ResponseEntity.ok(userService.updateLanguage(token, req.getLanguage()));
    }

    @PostMapping("/report")
    public ResponseEntity<Void> reportUser(
            @RequestHeader("Authorization") String token,
            @RequestBody ReportReq req
    ) {
        userService.reportUser(token, req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/image/upload")
    public ResponseEntity<String> uploadProfileImage(
            @RequestHeader("Authorization") String token,
            @RequestPart("file") MultipartFile file
    ) {
        String imageUrl = userService.updateProfileImage(token, file);
        return ResponseEntity.ok(imageUrl);
    }

    @DeleteMapping("image/delete")
    public ResponseEntity<Void> deleteProfileImage(
            @RequestHeader("Authorization") String token,
            @RequestParam("key") String key
    ) {
        userService.deleteProfileImage(token, key);
        return ResponseEntity.ok().build();
    }
}
