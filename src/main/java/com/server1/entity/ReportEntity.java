package com.server1.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "report")
@Getter
@NoArgsConstructor
public class ReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;  // 신고 ID

    @ManyToOne
    @JoinColumn(name = "reporter_id")
    private UserEntity reporter;

    @ManyToOne
    @JoinColumn(name = "reported_id")
    private UserEntity reported;

    @Column(nullable = false, length = 1000)
    private String reportContent;

    @Builder
    public ReportEntity(UserEntity reporter, UserEntity reported, String reportContent) {
        this.reporter = reporter;
        this.reported = reported;
        this.reportContent = reportContent;
    }
}
