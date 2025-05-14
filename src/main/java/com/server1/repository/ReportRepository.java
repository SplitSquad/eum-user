package com.server1.repository;

import com.server1.entity.ReportEntity;
import com.server1.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<ReportEntity, Long> {
    boolean existsByReporterAndReported(UserEntity reporter, UserEntity reported); // 추가
}
