package com.server1.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class UserPreferenceReq {
    @NotBlank private String nation;
    @NotBlank private String language;
    @NotBlank private String gender;
    @NotBlank private String visitPurpose;
    @NotBlank private String period;
    private String onBoardingPreference;
    private Boolean isOnBoardDone;
}