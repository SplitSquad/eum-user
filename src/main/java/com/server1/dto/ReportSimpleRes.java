package com.server1.dto;

import com.server1.entity.ReportEntity;
import com.server1.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReportSimpleRes {
    private Long reportId;
    private String reportContent;
    private UserEntity reporter;
    private Integer readStatus;


    public static ReportSimpleRes from(ReportEntity entity) {
        return new ReportSimpleRes(
                entity.getReportId(),
                entity.getReportContent(),
                entity.getReporter(),
                entity.getReadStatus()
        );
    }
}
