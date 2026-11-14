package com.library.audit;

import com.library.common.web.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditQueryService {

    private final AuditRepository auditRepository;

    public AuditQueryService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> search(AuditAction action, String q, int page, int size) {
        String query = q == null || q.isBlank() ? null : q.trim();
        return PageResponse.from(auditRepository.search(action, query,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200))), AuditEventResponse::from);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> recent(int count) {
        return auditRepository.search(null, null, PageRequest.of(0, count)).map(AuditEventResponse::from).getContent();
    }
}
