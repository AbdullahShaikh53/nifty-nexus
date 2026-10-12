package com.niftynexus.learning;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_badges")
public class UserBadge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "badge_code", nullable = false) private String badgeCode;
    @Column(name = "awarded_at", nullable = false) private LocalDateTime awardedAt = LocalDateTime.now();
    public Long getUserId() { return userId; } public void setUserId(Long v) { userId = v; }
    public String getBadgeCode() { return badgeCode; } public void setBadgeCode(String v) { badgeCode = v; }
    public LocalDateTime getAwardedAt() { return awardedAt; } public void setAwardedAt(LocalDateTime v) { awardedAt = v; }
}
