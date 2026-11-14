package com.library.dashboard;

import com.library.circulation.Eligibility;
import com.library.circulation.LoanResponse;
import com.library.hold.HoldResponse;
import com.library.member.UserResponse;
import com.library.notification.NotificationResponse;

import java.util.List;

public record MemberDashboard(
        UserResponse member,
        Eligibility eligibility,
        List<LoanResponse> currentLoans,
        List<HoldResponse> holds,
        long totalBorrowed,
        long outstandingFines,
        long finesPaid,
        List<NotificationResponse> notifications
) {
}
