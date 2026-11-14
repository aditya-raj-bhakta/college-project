package com.library.member;

import com.library.circulation.Eligibility;
import com.library.circulation.LoanResponse;
import com.library.fine.FineResponse;
import com.library.hold.HoldResponse;
import com.library.notification.NotificationResponse;

import java.util.List;

/** Everything about one member: used by the member detail page and by the desk when a card is scanned. */
public record MemberDetail(
        UserResponse member,
        Eligibility eligibility,
        List<LoanResponse> loans,
        List<HoldResponse> holds,
        List<FineResponse> fines,
        List<NotificationResponse> notifications
) {
}
