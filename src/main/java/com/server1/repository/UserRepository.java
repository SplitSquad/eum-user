package com.server1.repository;

import com.server1.entity.UserEntity;  // ← 내 엔티티 import
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
}
