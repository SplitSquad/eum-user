package com.server1.service;

import com.server1.dto.GoogleCalendarEventRequestDto;
import com.server1.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import util.JwtUtil;
import util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private final RedisUtil redisUtil;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    public List<Map<String, Object>> getUpcomingEvents(String token) {
        String accessToken = getAccessToken(token);
        try {
            URL url = new URL("https://www.googleapis.com/calendar/v3/calendars/primary/events");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> response = mapper.readValue(conn.getInputStream(), Map.class);

            return (List<Map<String, Object>>) response.get("items");
        } catch (Exception e) {
            throw new RuntimeException("캘린더 불러오기 실패", e);
        }
    }

    public ResponseEntity<?> insertEvent(String token, GoogleCalendarEventRequestDto eventDto) {
        String accessToken = getAccessToken(token);

        try {
            URL url = new URL("https://www.googleapis.com/calendar/v3/calendars/primary/events");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // JSON 구성
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("summary", eventDto.getSummary());
            eventData.put("location", eventDto.getLocation());
            eventData.put("description", eventDto.getDescription());

            Map<String, String> start = new HashMap<>();
            start.put("dateTime", eventDto.getStartDateTime());
            eventData.put("start", start);

            Map<String, String> end = new HashMap<>();
            end.put("dateTime", eventDto.getEndDateTime());
            eventData.put("end", end);

            // 전송
            ObjectMapper mapper = new ObjectMapper();
            try (OutputStream os = conn.getOutputStream()) {
                mapper.writeValue(os, eventData);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200 && responseCode != 201) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Google Calendar API 응답 오류", "code", responseCode));
            }

            Map<String, Object> response = mapper.readValue(conn.getInputStream(), Map.class);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "이벤트 삽입 실패", "message", e.getMessage()));
        }
    }

    public ResponseEntity<?> deleteEvent(String token, String eventId) {
        String accessToken = getAccessToken(token);

        try {
            URL url = new URL("https://www.googleapis.com/calendar/v3/calendars/primary/events/" + eventId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            int responseCode = conn.getResponseCode();
            if (responseCode == 204) { // 204 No Content 성공
                return ResponseEntity.ok(Map.of("message", "이벤트 삭제 성공"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Google Calendar API 삭제 실패", "code", responseCode));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "이벤트 삭제 실패", "message", e.getMessage()));
        }
    }

    public ResponseEntity<?> updateEvent(String token, String eventId, GoogleCalendarEventRequestDto eventDto) {
        String accessToken = getAccessToken(token);
        try {
            URL url = new URL("https://www.googleapis.com/calendar/v3/calendars/primary/events/" + eventId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST"); // PATCH 대신 POST
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH"); // 추가: PATCH 동작 지시
            conn.setDoOutput(true);
            // JSON 구성
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("summary", eventDto.getSummary());
            eventData.put("location", eventDto.getLocation());
            eventData.put("description", eventDto.getDescription());
            Map<String, String> start = new HashMap<>();
            start.put("dateTime", eventDto.getStartDateTime());
            eventData.put("start", start);
            Map<String, String> end = new HashMap<>();
            end.put("dateTime", eventDto.getEndDateTime());
            eventData.put("end", end);
            // JSON 보내기
            ObjectMapper mapper = new ObjectMapper();
            try (OutputStream os = conn.getOutputStream()) {
                mapper.writeValue(os, eventData);
            }
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Google Calendar API 수정 실패", "code", responseCode));
            }
            Map<String, Object> response = mapper.readValue(conn.getInputStream(), Map.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "이벤트 수정 중 오류 발생", "message", e.getMessage()));
        }
    }


    private String getAccessToken(String token) {
        Long userId = jwtUtil.getUserid(token);

        if (userId == null) {
            throw new RuntimeException("User email not found in header");
        }

        String email = userRepository.findById(userId).get().getEmail();

        String refreshToken = redisUtil.getRefreshToken(email);
        if (refreshToken == null) {
            throw new RuntimeException("Refresh 토큰 만료");
        }

        // refreshToken을 이용해 accessToken 갱신
        try {
            GoogleTokenResponse tokenResponse = new GoogleRefreshTokenRequest(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    refreshToken,
                    clientId,
                    clientSecret
            ).execute();

            return tokenResponse.getAccessToken();
        } catch (IOException e) {
            throw new RuntimeException("구글 accessToken 재발급 실패", e);
        }
    }
}
