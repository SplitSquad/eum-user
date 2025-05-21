package com.server1.dto;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserReq {
    private String name;
    private String address;
    private String phoneNumber;
    private String birthday;
    private String email;

    private String password;
}