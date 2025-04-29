package com.server1.service;

import com.server1.dto.KafkaUser;
import com.server1.dto.UserPreferenceRes;
import com.server1.dto.UserReq;
import com.server1.dto.UserRes;
import com.server1.entity.UserEntity;
import com.server1.entity.UserPreferenceEntity;
import com.server1.repository.UserPreferenceRepository;
import com.server1.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository prefRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public UserRes getProfile(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
        return UserRes.from(user);
    }

    @Transactional
    public UserRes updateProfile(String email, UserReq req) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        user.setName(req.getName());
        user.setAddress(req.getAddress());
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
                user.getProfileImagePath(),
                user.getAddress(),
                user.getSignedAt(),
                user.getIsDeactivate(),
                user.getRole()
        );
    }

    @Transactional
    public UserPreferenceRes updateLanguage(String email, String language) {
        UserEntity user = userRepository.findByEmail(email)
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
}
