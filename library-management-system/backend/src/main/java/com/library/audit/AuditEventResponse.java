package com.library.audit;

import java.time.LocalDateTime;

public record AuditEventResponse(Long id, LocalDateTime occurredAt, String actorName, AuditAction action,
                                 String targetType, Long targetId, String summary) {

    public static AuditEventResponse from(AuditEvent e) {
        return new AuditEventResponse(e.getId(), e.getOccurredAt(), e.getActorName(), e.getAction(),
                e.getTargetType(), e.getTargetId(), e.getSummary());
    }
}
