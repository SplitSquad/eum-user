package com.server1.dto;

import com.server1.entity.UserEntity;
import com.server1.entity.UserPreferenceEntity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserFullRes {
    private Long userId;
    private String email;
    private String name;
    private String phoneNumber;
    private String birthday;
    private String profileImagePath;
    private String address;
    private LocalDateTime signedAt;
    private String loginType;
    private String role;
    private Integer nReported;
    private Integer deactivateCount;

    // Preference 정보
    private String nation;
    private String language;
    private String gender;
    private String visitPurpose;
    private String period;
    private String onBoardingPreference;
    private Boolean isOnBoardDone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserFullRes from(UserEntity user, UserPreferenceEntity pref) {
        return UserFullRes.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .birthday(user.getBirthday())
                .profileImagePath(user.getProfileImagePath())
                .address(user.getAddress())
                .signedAt(user.getSignedAt())
                .loginType(user.getLoginType())
                .role(user.getRole())
                .nReported(user.getNReported())
                .deactivateCount(user.getDeactivateCount())
                .nation(pref.getNation())
                .language(pref.getLanguage())
                .gender(pref.getGender())
                .visitPurpose(pref.getVisitPurpose())
                .period(pref.getPeriod())
                .onBoardingPreference(pref.getOnBoardingPreference())
                .isOnBoardDone(pref.getIsOnBoardDone())
                .createdAt(pref.getCreatedAt())
                .updatedAt(pref.getUpdatedAt())
                .build();
    }
}
