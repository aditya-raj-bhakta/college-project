package com.library.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** An append-only record of who did what. The actor's name is copied so it survives renames. */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    /** Null for the system (scheduled jobs). */
    private Long actorId;

    @Column(nullable = false, length = 100)
    private String actorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    @Column(nullable = false, length = 30)
    private String targetType;

    private Long targetId;

    @Column(nullable = false, length = 500)
    private String summary;

    protected AuditEvent() {
    }

    public AuditEvent(LocalDateTime occurredAt, Long actorId, String actorName, AuditAction action,
                      String targetType, Long targetId, String summary) {
        this.occurredAt = occurredAt;
        this.actorId = actorId;
        this.actorName = actorName;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.summary = summary.length() > 500 ? summary.substring(0, 500) : summary;
    }

    public Long getId() { return id; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public Long getActorId() { return actorId; }
    public String getActorName() { return actorName; }
    public AuditAction getAction() { return action; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getSummary() { return summary; }
}
