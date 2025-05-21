package com.server1.dto;

import com.server1.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRes {
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

    public static UserRes from(UserEntity entity) {
        return new UserRes(
                entity.getUserId(),
                entity.getEmail(),
                entity.getName(),
                entity.getPhoneNumber(),
                entity.getBirthday(),
                entity.getProfileImagePath(),
                entity.getAddress(),
                entity.getSignedAt(),
                entity.getLoginType(),
                entity.getRole(),
                entity.getNReported(),
                entity.getDeactivateCount()
        );
    }
}
