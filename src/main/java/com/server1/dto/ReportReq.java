package com.server1.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportReq {
    private Long reportedId;      // 신고당한 사람 ID
    private String reportContent; // 신고 내용
}