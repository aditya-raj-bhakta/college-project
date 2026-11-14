package com.library.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditRepository extends JpaRepository<AuditEvent, Long> {

    @Query("""
            SELECT e FROM AuditEvent e
            WHERE (:action IS NULL OR e.action = :action)
              AND (:q IS NULL
                   OR LOWER(e.summary) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(e.actorName) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY e.occurredAt DESC, e.id DESC
            """)
    Page<AuditEvent> search(@Param("action") AuditAction action, @Param("q") String q, Pageable pageable);
}
