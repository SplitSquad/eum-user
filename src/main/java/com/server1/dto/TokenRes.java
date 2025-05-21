package com.server1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenRes {
    private String token;
    private String email;
    private String role;
    private String name;
    private String loginType;
    private Boolean isOnBoardDone;
}
