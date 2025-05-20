package com.server1.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DeactivateReq {
    private Long userId;
    private int minutes;
}
