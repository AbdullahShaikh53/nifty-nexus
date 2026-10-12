package com.niftynexus.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String email;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role = Role.USER;
    @Column(nullable = false) private boolean blocked = false;
    @Column(name = "fno_unlocked", nullable = false) private boolean fnoUnlocked = false;
    @Column(name = "risk_score", nullable = false) private int riskScore = 50;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getEmail() { return email; } public void setEmail(String v) { email = v; }
    public String getPasswordHash() { return passwordHash; } public void setPasswordHash(String v) { passwordHash = v; }
    public Role getRole() { return role; } public void setRole(Role v) { role = v; }
    public boolean isBlocked() { return blocked; } public void setBlocked(boolean v) { blocked = v; }
    public boolean isFnoUnlocked() { return fnoUnlocked; } public void setFnoUnlocked(boolean v) { fnoUnlocked = v; }
    public int getRiskScore() { return riskScore; } public void setRiskScore(int v) { riskScore = v; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { createdAt = v; }
}
