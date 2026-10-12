package com.niftynexus.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes audit rows for logins, orders, admin actions and simulate-open usage (FR-62, FR-75). */
@Service
public class AuditService {
    private final AuditLogRepository repo;
    public AuditService(AuditLogRepository repo) { this.repo = repo; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String actor, String action, String detail) {
        AuditLog a = new AuditLog();
        a.setActor(actor); a.setAction(action);
        a.setDetail(detail != null && detail.length() > 490 ? detail.substring(0, 490) : detail);
        repo.save(a);
    }
}
