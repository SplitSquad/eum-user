package com.server1.service;

import com.server1.dto.*;
import util.JwtUtil;
import com.server1.entity.UserEntity;
import com.server1.entity.UserPreferenceEntity;
import com.server1.repository.UserPreferenceRepository;
import com.server1.repository.UserRepository;
import util.RedisUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final UserRepository userRepository;
    private final UserPreferenceRepository prefRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final UserPreferenceRepository userPreferenceRepository;
    private final ObjectMapper objectMapper;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;


    public String generateGoogleAuthUrl() {
        String scope = "email profile https://www.googleapis.com/auth/calendar";
        return "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + googleClientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&access_type=offline" +
                "&include_granted_scopes=true" +
                "&scope=" + scope +
                "&prompt=consent";
    }

    public TokenRes login(String code, HttpServletResponse res) {
        try {
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    "https://oauth2.googleapis.com/token",
                    googleClientId,
                    googleClientSecret,
                    code,
                    redirectUri
            ).execute();

            GoogleIdToken idToken = tokenResponse.parseIdToken();
            String email = idToken.getPayload().getEmail();
            String name = (String) idToken.getPayload().get("name");
            String pictureUrl = (String) idToken.getPayload().get("picture");
            String googleRefreshToken = tokenResponse.getRefreshToken();

            boolean isNewUser = false;

            Optional<UserEntity> optionalUser = userRepository.findByEmail(email);
            UserEntity user;

            if (optionalUser.isPresent()) {
                user = optionalUser.get();
            } else {
                user = userRepository.save(UserEntity.builder()
                        .email(email)
                        .name(name)
                        .signedAt(LocalDateTime.now())
                        .profileImagePath(pictureUrl)
                        .isDeactivate(false)
                        .role("ROLE_USER")
                        .build());
                isNewUser = true;
            }


            UserPreferenceEntity pref = prefRepository.findByUser(user).orElseGet(() -> prefRepository.save(
                    UserPreferenceEntity.builder()
                            .user(user)
                            .nation("")
                            .language("")
                            .gender("")
                            .visitPurpose("")
                            .period("")
                            .onBoardingPreference("{}")
                            .isOnBoardDone(false)
                            .build()
            ));

            boolean isOnBoardDone = pref.getIsOnBoardDone();

            KafkaUser kafkaDto = new KafkaUser(
                    user.getUserId(),
                    user.getName(),
                    "", "", user.getRole(),
                    user.getAddress()
            );

            if (isNewUser) {
                try {
                    kafkaTemplate.send("createUser", objectMapper.writeValueAsString(kafkaDto));
                } catch (JsonProcessingException e) {
                    log.error("Kafka 직렬화 실패", e);
                }
            }

            // Redis 저장 및 refreshToken 쿠키 설정
            redisUtil.setRefreshToken(email, googleRefreshToken, Duration.ofDays(7).toMillis());

            // ResponseCookie 대신 Cookie 사용
            Cookie refreshCookie = new Cookie("refreshToken", googleRefreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge((int) Duration.ofDays(7).getSeconds());

            res.addCookie(refreshCookie);

            String accessToken = jwtUtil.generateToken(user.getUserId(), user.getRole(), Duration.ofMinutes(15).toMillis());

            res.addHeader("Authorization", accessToken);

            TokenRes tokenRes = new TokenRes();
            tokenRes.setEmail(user.getEmail());
            tokenRes.setRole(user.getRole());

            return tokenRes;

        } catch (IOException e) {
            throw new ResponseStatusException(UNAUTHORIZED, "Google login failed");
        }
    }


    public TokenRes refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie c: request.getCookies()) {
                if ("refreshToken".equals(c.getName())) {
                    refreshToken = c.getValue();
                }
            }
        }
        if (refreshToken == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Refresh token not found");
        }

        try {
            GoogleTokenResponse tokenResponse = new GoogleRefreshTokenRequest(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    refreshToken,
                    googleClientId,
                    googleClientSecret
            ).execute();

            GoogleIdToken idToken = GoogleIdToken.parse(
                    JacksonFactory.getDefaultInstance(),
                    tokenResponse.getIdToken()
            );
            String email = idToken.getPayload().getEmail();

            // Redis에서 refresh token 검증
            String stored = redisUtil.getRefreshToken(email);
            if (stored == null || !stored.equals(refreshToken)) {
                throw new ResponseStatusException(UNAUTHORIZED, "Refresh token invalid");
            }

            // 사용자 정보 조회
            UserEntity user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

            // 새로운 access token 발급
            String newAccessToken = jwtUtil.generateToken(user.getUserId(), user.getRole(), Duration.ofMinutes(15).toMillis());
            response.addHeader("Authorization", newAccessToken);

            TokenRes tokenRes = new TokenRes();
            tokenRes.setEmail(user.getEmail());
            tokenRes.setRole(user.getRole());

            return tokenRes;

        } catch (Exception e) {
            throw new ResponseStatusException(UNAUTHORIZED, "Refresh token invalid");
        }
    }

    public ResponseEntity<CommonRes> logout(String accessToken) {
        Long userid = jwtUtil.getUserid(accessToken);
        redisUtil.deleteRefreshToken(userRepository.findById(userid).get().getEmail());
        return ResponseEntity.ok(new CommonRes(true));
    }


    @Transactional
    public ResponseEntity<CommonRes> deleteUser(String accessToken) {
        Long userid = jwtUtil.getUserid(accessToken);

        UserEntity user = userRepository.findById(userid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        redisUtil.deleteRefreshToken(user.getEmail());
        userPreferenceRepository.deleteByUser(user);

        KafkaUser dto = new KafkaUser(
                user.getUserId(),
                user.getName(),
                "", "", "DELETED",
                user.getAddress()
        );
        try {
            kafkaTemplate.send("deleteUser", objectMapper.writeValueAsString(dto));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        userRepository.delete(user);

        return ResponseEntity.ok(new CommonRes(true));
    }
}
