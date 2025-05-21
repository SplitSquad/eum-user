package com.server1.controller;

import com.server1.dto.TokenRes;
import com.server1.dto.LogoutReq;
import com.server1.dto.CommonRes;
import com.server1.dto.UserReq;
import com.server1.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    
    @GetMapping("/url")
    public ResponseEntity<Map<String, String>> getGoogleAuthUrl() {
        String authUrl = authService.generateGoogleAuthUrl();
        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    @GetMapping("/login")
    public TokenRes googleCallback(@RequestParam("code") String code, HttpServletResponse res) {
        return authService.login(code, res);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRes> refresh(HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(authService.refreshToken(request, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<CommonRes> logout(@RequestHeader("Authorization") String accessToken) {
        return authService.logout(accessToken);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<CommonRes> deleteUser(@RequestHeader("Authorization") String accessToken) {
        return authService.deleteUser(accessToken);
    }

    @PostMapping("/common/join")
    public ResponseEntity<?> join(@RequestBody UserReq userReq){
        return authService.commonJoin(userReq);
    }

    @PostMapping("/common/login")
    public ResponseEntity<?> login(@RequestBody UserReq userReq, HttpServletResponse res){
        return authService.commonLogin(userReq, res);
    }
}
