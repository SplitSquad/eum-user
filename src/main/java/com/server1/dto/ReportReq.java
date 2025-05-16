package com.server1.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportReq {
    private Long reportedId;         // 신고당한 사용자 ID
    private String reportContent;    // 신고 사유 내용
    private String serviceType;      // 어떤 서비스(Community, Discussion 등)
    private String targetType;       // 대상 종류(Post, Comment 등)
    private Long contentId;           // 신고한 글/댓글 ID
}