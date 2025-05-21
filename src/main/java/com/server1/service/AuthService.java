package com.server1.service;

import com.server1.dto.*;
import org.springframework.beans.factory.config.Scope;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;
    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExp;
    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExp;

    public String generateGoogleAuthUrl() {  // 구글 로그인 폼
        String scope = "email profile https://www.googleapis.com/auth/calendar https://www.googleapis.com/auth/user.phonenumbers.read https://www.googleapis.com/auth/user.birthday.read https://www.googleapis.com/auth/user.addresses.read";
        return "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + googleClientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&access_type=offline" +
                "&include_granted_scopes=true" +
                "&scope=" + scope +
                "&prompt=consent";
    }

    private Map<String, String> getPhoneBirthdayAddress(String accessToken) {
        try {
            URL url = new URL("https://people.googleapis.com/v1/people/me?personFields=phoneNumbers,birthdays,addresses");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> response = mapper.readValue(conn.getInputStream(), Map.class);

            String phoneNumber = ((List<Map<String, Object>>) response.getOrDefault("phoneNumbers", List.of()))
                    .stream().findFirst()
                    .map(phone -> (String) phone.get("value"))
                    .orElse("");

            String birthday = ((List<Map<String, Object>>) response.getOrDefault("birthdays", List.of()))
                    .stream().findFirst()
                    .map(b -> {
                        Map<String, Object> date = (Map<String, Object>) b.get("date");
                        if (date == null) return "";
                        int year = (int) date.getOrDefault("year", 0);
                        int month = (int) date.getOrDefault("month", 0);
                        int day = (int) date.getOrDefault("day", 0);
                        return String.format("%04d-%02d-%02d", year, month, day);
                    }).orElse("");

            String address = ((List<Map<String, Object>>) response.getOrDefault("addresses", List.of()))
                    .stream().findFirst()
                    .map(addr -> (String) addr.get("formattedValue"))
                    .orElse("");

            return Map.of(
                    "phoneNumber", phoneNumber,
                    "birthday", birthday,
                    "address", address
            );

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("phoneNumber", "", "birthday", "", "address", "");
        }
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

            Map<String, String> info = getPhoneBirthdayAddress(tokenResponse.getAccessToken());
            String phoneNumber = info.get("phoneNumber");
            String birthday = info.get("birthday");
            String address = info.get("address");

            boolean isNewUser = false;

            Optional<UserEntity> optionalUser = userRepository.findByEmail(email);
            UserEntity user;

            if (optionalUser.isPresent()) {
                user = optionalUser.get();
                user.setPhoneNumber(phoneNumber);
                user.setBirthday(birthday);
                userRepository.save(user);
            } else {
                user = userRepository.save(UserEntity.builder()
                        .email(email)
                        .name(name)
                        .profileImagePath(pictureUrl)
                        .phoneNumber(phoneNumber)
                        .birthday(birthday)
                        .address(address)
                        .signedAt(LocalDateTime.now())
                        .loginType("구글")
                        .role("ROLE_USER")
                        .nReported(0)
                        .deactivateCount(0)
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
            redisUtil.setRefreshToken(email, googleRefreshToken,refreshTokenExp);

            // ResponseCookie 대신 Cookie 사용
            /**Cookie refreshCookie = new Cookie("refreshToken", googleRefreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge((int) Duration.ofDays(7).getSeconds());

            res.addCookie(refreshCookie);*/

            String cookieValue = "refreshToken=" + googleRefreshToken +
                    "; Max-Age=" + Duration.ofDays(refreshTokenExp).getSeconds() +
                    "; Path=/; HttpOnly; Secure; SameSite=None";

            res.addHeader("Set-Cookie", cookieValue);

            String accessToken = jwtUtil.generateToken(user.getUserId(), user.getEmail() ,user.getRole(),accessTokenExp) ;

            TokenRes tokenRes = new TokenRes();
            tokenRes.setEmail(user.getEmail());
            tokenRes.setRole(user.getRole());
            tokenRes.setToken(accessToken);
            tokenRes.setName(user.getName());
            tokenRes.setIsOnBoardDone(isOnBoardDone);
            tokenRes.setLoginType(user.getLoginType());

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
            throw new ResponseStatusException(UNAUTHORIZED, "Refresh 없음");
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
            String newAccessToken = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole(), accessTokenExp);
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

    public ResponseEntity<?> commonJoin(UserReq userReq) {
        UserEntity user = userRepository.findByEmail(userReq.getEmail()).orElse(null);
        if (user != null) {
            return ResponseEntity.badRequest().body("Email already in use");
        }

        user = UserEntity.builder()
                .email(userReq.getEmail())
                .name(userReq.getName())
                .address(userReq.getAddress())
                .birthday(userReq.getBirthday())
                .role("ROLE_USER")
                .loginType("일반")
                .signedAt(LocalDateTime.now())
                .deactivateCount(0)
                .nReported(0)
                .phoneNumber(userReq.getPhoneNumber())
                .password(bCryptPasswordEncoder.encode(userReq.getPassword()))
                .build();
        userRepository.save(user);

        UserPreferenceEntity pref = UserPreferenceEntity.builder()
                .user(user)
                .nation("")
                .language("")
                .gender("")
                .visitPurpose("")
                .period("")
                .onBoardingPreference("{}")
                .isOnBoardDone(false)
                .build();

        userPreferenceRepository.save(pref);

        KafkaUser kafkaDto = new KafkaUser(
                user.getUserId(),
                user.getName(),
                "", "", user.getRole(),
                user.getAddress()
        );

        try {
            kafkaTemplate.send("createUser", objectMapper.writeValueAsString(kafkaDto));
        } catch (JsonProcessingException e) {
            log.error("Kafka 직렬화 실패", e);
        }
        return ResponseEntity.ok("User joined");
    }

    public ResponseEntity<?> commonLogin(UserReq userReq, HttpServletResponse res) {
        UserEntity user = userRepository.findByEmail(userReq.getEmail()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        if(!bCryptPasswordEncoder.matches(userReq.getPassword(), user.getPassword())){
            return ResponseEntity.badRequest().body("Wrong password");
        }


        String refreshToken = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole(), refreshTokenExp);
        redisUtil.setRefreshToken(user.getEmail(), refreshToken ,refreshTokenExp);


        String cookieValue = "refreshToken=" + refreshToken +
                "; Max-Age=" + Duration.ofDays(refreshTokenExp).getSeconds() +
                "; Path=/; HttpOnly; Secure; SameSite=None";

        res.addHeader("Set-Cookie", cookieValue);

        String accessToken = jwtUtil.generateToken(user.getUserId(), user.getEmail() ,user.getRole(),accessTokenExp) ;

        UserPreferenceEntity pref = userPreferenceRepository.findByUser(user).get();

        TokenRes tokenRes = new TokenRes();
        tokenRes.setEmail(user.getEmail());
        tokenRes.setRole(user.getRole());
        tokenRes.setToken(accessToken);
        tokenRes.setName(user.getName());
        tokenRes.setIsOnBoardDone(pref.getIsOnBoardDone());
        tokenRes.setLoginType(user.getLoginType());

        return ResponseEntity.ok(tokenRes);
    }

}

