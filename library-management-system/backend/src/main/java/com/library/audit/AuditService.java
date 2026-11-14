package com.library.audit;

import com.library.member.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Records audit events inside the caller's transaction, so an action and its
 * audit entry are saved together or not at all.
 */
@Service
public class AuditService {

    private final AuditRepository auditRepository;
    private final Clock clock;

    public AuditService(AuditRepository auditRepository, Clock clock) {
        this.auditRepository = auditRepository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(User actor, AuditAction action, String targetType, Long targetId, String summary) {
        auditRepository.save(new AuditEvent(LocalDateTime.now(clock),
                actor == null ? null : actor.getId(),
                actor == null ? "System" : actor.getName(),
                action, targetType, targetId, summary));
    }
}
