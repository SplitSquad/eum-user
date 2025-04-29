package com.server1.repository;

import com.server1.entity.UserPreferenceEntity;
import com.server1.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreferenceEntity, Long> {
    void deleteByUser(UserEntity user);

    // 특정 사용자 설정 조회
    Optional<UserPreferenceEntity> findByUser(UserEntity user);

    // (필요시) 사용자에 연결된 모든 preference 조회
    List<UserPreferenceEntity> findAllByUser(UserEntity user);
}
