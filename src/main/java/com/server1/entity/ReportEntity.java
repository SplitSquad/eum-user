package com.server1.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @ManyToOne
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserEntity reporter;

    @ManyToOne
    @JoinColumn(name = "reported_id", nullable = false)
    private UserEntity reported;

    @Column(nullable = false, length = 1000)
    private String reportContent;

    @Column(nullable = false)
    private String serviceType;   // Community, Discussion

    @Column(nullable = false)
    private String targetType;    // Post, Comment

    @Column(nullable = false)
    private Long contentId;       // 글/댓글 ID

    @Column(nullable = false)
    private Integer readStatus = 0;
}

