package com.server1.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
@Entity
@Table(name = "USER")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private Integer nReported = 0;

    @Column(nullable = false)
    private Integer deactivateCount = 0;

    private String phoneNumber;
    private String birthday;
    private String profileImagePath;
    private String address;
    private LocalDateTime signedAt;
    private Boolean isDeactivate;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private UserPreferenceEntity userPreference;
}
