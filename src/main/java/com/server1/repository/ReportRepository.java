package com.server1.repository;

import com.server1.entity.ReportEntity;
import com.server1.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<ReportEntity, Long> {
    boolean existsByReporterAndReportedAndServiceTypeAndTargetTypeAndContentId(
            UserEntity reporter, UserEntity reported, String serviceType, String targetType, Long contentId
    );
    List<ReportEntity> findByReported(UserEntity reported);
}
