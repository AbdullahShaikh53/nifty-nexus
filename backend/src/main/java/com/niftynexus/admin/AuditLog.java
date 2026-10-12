package com.niftynexus.admin;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String actor;
    @Column(nullable = false) private String action;
    private String detail;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getActor() { return actor; } public void setActor(String v) { actor = v; }
    public String getAction() { return action; } public void setAction(String v) { action = v; }
    public String getDetail() { return detail; } public void setDetail(String v) { detail = v; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { createdAt = v; }
}
