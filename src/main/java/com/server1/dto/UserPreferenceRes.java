package com.server1.dto;

import com.server1.entity.UserPreferenceEntity;
import lombok.AllArgsConstructor; import lombok.Getter;
import java.time.LocalDateTime;

@Getter @AllArgsConstructor
public class UserPreferenceRes {
    private Long preferenceId;
    private Long userId;
    private String nation;
    private String language;
    private String gender;
    private String visitPurpose;
    private String period;
    private String onBoardingPreference;
    private Boolean isOnBoardDone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserPreferenceRes fromEntity(UserPreferenceEntity e) {
        return new UserPreferenceRes(
                e.getPreferenceId(),
                e.getUser().getUserId(),
                e.getNation(),
                e.getLanguage(),
                e.getGender(),
                e.getVisitPurpose(),
                e.getPeriod(),
                e.getOnBoardingPreference(),
                e.getIsOnBoardDone(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}