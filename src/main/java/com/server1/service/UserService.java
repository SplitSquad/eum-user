package com.server1.service;

import com.server1.dto.*;
import com.server1.entity.UserEntity;
import com.server1.entity.UserPreferenceEntity;
import com.server1.entity.ReportEntity;
import com.server1.repository.UserPreferenceRepository;
import com.server1.repository.UserRepository;
import com.server1.repository.ReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import util.JwtUtil;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository prefRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final ReportRepository reportRepository;

    private final AwsS3Service awsS3Service;

    public UserRes getProfile(String token) {
        Long userId = jwtUtil.getUserid(token);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        return UserRes.from(user);
    }

    @Transactional
    public UserRes updateProfile(String token, UserReq req) {
        Long userId = jwtUtil.getUserid(token);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        user.setName(req.getName());
        user.setAddress(req.getAddress());
        user.setBirthday(req.getBirthday());
        user.setPhoneNumber(req.getPhoneNumber());
        user = userRepository.save(user);

        UserPreferenceEntity pref = prefRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "preference not found"));

        KafkaUser dto = new KafkaUser(
                user.getUserId(),
                user.getName(),
                pref.getLanguage(),
                pref.getNation(),
                user.getRole(),
                user.getAddress()
        );
        try {
            kafkaTemplate.send("updateUser", objectMapper.writeValueAsString(dto));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return new UserRes(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getPhoneNumber(),
                user.getBirthday(),
                user.getProfileImagePath(),
                user.getAddress(),
                user.getSignedAt(),
                user.getLoginType(),
                user.getRole(),
                user.getNReported(),
                user.getDeactivateCount()
        );
    }

    @Transactional
    public UserPreferenceRes updateLanguage(String token, String language) {
        Long userId = jwtUtil.getUserid(token);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        UserPreferenceEntity pref = prefRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "preference not found"));

        pref.setLanguage(language);
        prefRepository.save(pref);

        KafkaUser dto = new KafkaUser(
                user.getUserId(),
                user.getName(),
                pref.getNation(),
                language,
                user.getRole(),
                user.getAddress()
        );
        try {
            kafkaTemplate.send("updateLanguage", objectMapper.writeValueAsString(dto));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return UserPreferenceRes.fromEntity(pref);
    }

    @Transactional
    public void reportUser(String token, ReportReq req) {
        Long reporterId = jwtUtil.getUserid(token); // JWT로 신고자 ID 추출

        UserEntity reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신고자 없음"));

        UserEntity reported = userRepository.findById(req.getReportedId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "피신고자 없음"));

        boolean alreadyReported = reportRepository.existsByReporterAndReportedAndServiceTypeAndTargetTypeAndContentId(
                reporter, reported, req.getServiceType(), req.getTargetType(), req.getContentId()
        );

        if (alreadyReported) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 신고한 글/댓글입니다.");
        }

        reportRepository.save(
                ReportEntity.builder()
                        .reporter(reporter)
                        .reported(reported)
                        .reportContent(req.getReportContent())
                        .serviceType(req.getServiceType())
                        .targetType(req.getTargetType())
                        .contentId(req.getContentId())
                        .build()
        );

        reported.setNReported(reported.getNReported() + 1);
        userRepository.save(reported);
    }

    @Transactional
    public String updateProfileImage(String token, MultipartFile file) {
        Long userId = jwtUtil.getUserid(token);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        String imageUrl = awsS3Service.upload(file); // 분리된 서비스 호출
        user.setProfileImagePath(imageUrl);
        userRepository.save(user);

        return imageUrl;
    }

    @Transactional
    public void deleteProfileImage(String token, String key) {
        Long userId = jwtUtil.getUserid(token);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        awsS3Service.delete(key);
        user.setProfileImagePath(null); // 또는 ""로
        userRepository.save(user);
    }
}
