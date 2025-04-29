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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {
    private final UserRepository userRepository;
    private final UserPreferenceRepository prefRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public UserPreferenceRes getPreference(Authentication auth) {
        String email = (String) auth.getPrincipal();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));

        UserPreferenceEntity pref = prefRepo.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "preference not found"));

        return UserPreferenceRes.fromEntity(pref);
    }

    public UserPreferenceRes saveOrUpdate(Authentication auth, UserPreferenceReq req) {
        String email = (String) auth.getPrincipal();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));

        UserPreferenceEntity pref = prefRepo.findByUser(user)
                .orElse(UserPreferenceEntity.builder().user(user).build());

        pref.setNation(req.getNation());
        pref.setLanguage(req.getLanguage());
        pref.setGender(req.getGender());
        pref.setVisitPurpose(req.getVisitPurpose());
        pref.setPeriod(req.getPeriod());
        pref.setOnBoardingPreference(req.getOnBoardingPreference());
        pref.setIsOnBoardDone(req.getIsOnBoardDone());

        pref = prefRepo.save(pref);

        // Kafka: 선호도 변경 이벤트도 updateUser 토픽으로 발행
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
