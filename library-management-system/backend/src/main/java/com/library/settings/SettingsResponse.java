package com.library.settings;

import com.library.member.MemberTypeResponse;

import java.util.List;

public record SettingsResponse(
        int holdPickupDays,
        int lostItemFee,
        int reminderDaysBefore,
        Long defaultMemberTypeId,
        List<MemberTypeResponse> memberTypes
) {
}
