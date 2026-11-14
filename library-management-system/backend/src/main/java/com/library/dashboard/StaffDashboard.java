package com.library.dashboard;

import com.library.audit.AuditEventResponse;

import java.util.List;
import java.util.Map;

/**
 * @param copiesByStatus every copy in the collection by status, for the stack map
 */
public record StaffDashboard(
        long titles,
        long members,
        long openLoans,
        long overdueLoans,
        long holdsWaiting,
        long holdsReady,
        long outstandingFines,
        long finesCollected,
        Map<String, Long> copiesByStatus,
        List<AuditEventResponse> recentActivity
) {
}
