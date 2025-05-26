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
    private String profileImagePath;
    private String role;
    private Integer nReported;
    private Integer deactivateCount;
    private boolean isDeactivated;

    public static UserFullRes from(UserEntity user, boolean isDeactivated) {
        return UserFullRes.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .profileImagePath(user.getProfileImagePath())
                .role(user.getRole())
                .nReported(user.getNReported())
                .deactivateCount(user.getDeactivateCount())
                .isDeactivated(isDeactivated)
                .build();
    }
}
