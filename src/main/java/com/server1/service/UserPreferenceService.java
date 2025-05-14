package com.server1.service;

import com.server1.dto.UserPreferenceReq;
import com.server1.dto.UserPreferenceRes;
import com.server1.entity.UserEntity;
import com.server1.entity.UserPreferenceEntity;
import com.server1.dto.KafkaUser;
import com.server1.repository.UserPreferenceRepository;
import com.server1.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import util.JwtUtil;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {
    private final UserRepository userRepository;
    private final UserPreferenceRepository prefRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;

    public UserPreferenceRes getPreference(String token) {
        Long userId = jwtUtil.getUserid(token);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));

        UserPreferenceEntity pref = prefRepo.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "preference not found"));

        return UserPreferenceRes.fromEntity(pref);
    }

    public UserPreferenceRes saveOrUpdate(String token, UserPreferenceReq req) {
        Long userId = jwtUtil.getUserid(token);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));

        UserPreferenceEntity pref = prefRepo.findByUser(user)
                .orElse(UserPreferenceEntity.builder().user(user).build());

        if (req.getNation() != null) pref.setNation(req.getNation());
        if (req.getLanguage() != null) pref.setLanguage(req.getLanguage());
        if (req.getGender() != null) pref.setGender(req.getGender());
        if (req.getVisitPurpose() != null) pref.setVisitPurpose(req.getVisitPurpose());
        if (req.getPeriod() != null) pref.setPeriod(req.getPeriod());
        if (req.getOnBoardingPreference() != null) pref.setOnBoardingPreference(req.getOnBoardingPreference());
        if (req.getIsOnBoardDone() != null) pref.setIsOnBoardDone(req.getIsOnBoardDone());

        pref = prefRepo.save(pref);

        KafkaUser prefDto = new KafkaUser(
                user.getUserId(),
                user.getName(),
                pref.getNation(),
                pref.getLanguage(),
                user.getRole(),
                user.getAddress()
        );
        try {
            kafkaTemplate.send(
                    "updateUser",
                    objectMapper.writeValueAsString(prefDto)
            );
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return UserPreferenceRes.fromEntity(pref);
    }

}
