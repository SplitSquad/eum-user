package com.server1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KafkaUser {
    private Long userId;
    private String name;
    private String nation;
    private String language;
    private String role;
    private String address;
}