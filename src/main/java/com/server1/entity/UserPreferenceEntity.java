package com.server1.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_PREFERENCE")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long preferenceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false, unique = true)
    @JsonIgnore
    private UserEntity user;

    @Column(nullable = false)
    private String nation;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false)
    private String visitPurpose;

    @Column(nullable = false)
    private String period;

    @Column(columnDefinition = "json")
    private String onBoardingPreference;

    @Column(nullable = false)
    private Boolean isOnBoardDone;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
