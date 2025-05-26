package com.server1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server1.dto.KafkaDeactivate;
import com.server1.dto.ReportSimpleRes;
import com.server1.dto.UserFullRes;
import com.server1.entity.ReportEntity;
import com.server1.entity.UserEntity;
import com.server1.entity.UserPreferenceEntity;
import com.server1.repository.ReportRepository;
import com.server1.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import util.JwtUtil;
import util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final RedisUtil redisUtil;
    private final JwtUtil jwtUtil;

    private void validateAdmin(String token) {
        String pureToken = token.replace("Bearer ", "");
        String role = jwtUtil.getRole(pureToken);
        if (!"ROLE_ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 없습니다.");
        }
    }

    public List<ReportSimpleRes> getReportsByReportedId(Long userId, String token) {
        validateAdmin(token);
        UserEntity reported = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        return reportRepository.findByReported(reported)
                .stream()
                .map(ReportSimpleRes::from)
                .collect(Collectors.toList());
    }

    public ReportEntity getReportDetail(Long reportId, String token) {
        validateAdmin(token);
        ReportEntity report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다."));

        if (report.getReadStatus() == 0) {
            report.setReadStatus(1);
            reportRepository.save(report);
        }

        return report;
    }

    @Transactional
    public void deactivateTemporarily(Long userId, int minutes, String token) {
        validateAdmin(token);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        user.setDeactivateCount(user.getDeactivateCount() + 1);
        userRepository.save(user);

        redisUtil.setTempDeactivate(user.getEmail(), "true", minutes);

        KafkaDeactivate event = new KafkaDeactivate(user.getUserId(), 1);
        try {
            kafkaTemplate.send("deactivate", objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.error("Kafka 직렬화 실패", e);
        }
    }


    @Transactional
    public void promoteUserToAdmin(String email, String token) {
        validateAdmin(token);
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        user.setRole("ROLE_ADMIN");
        userRepository.save(user);
    }

    @Transactional
    public List<UserFullRes> getAllUserInfos(String token) {
        validateAdmin(token);
        return userRepository.findAll().stream()
                .map(user -> {
                    boolean isDeactivated = redisUtil.getTempDeactivate(user.getEmail()) != null;
                    return UserFullRes.from(user, isDeactivated);
                })
                .collect(Collectors.toList());
    }


}
