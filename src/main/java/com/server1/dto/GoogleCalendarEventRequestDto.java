package com.server1.dto;

import lombok.Data;

@Data
public class GoogleCalendarEventRequestDto {
    private String summary;
    private String location;
    private String description;
    private String startDateTime; // ISO 형식 (예: 2025-05-01T10:00:00+09:00)
    private String endDateTime;
}