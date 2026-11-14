package com.library.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SettingsRequest(
        @Min(value = 1, message = "At least 1 day") @Max(value = 30, message = "At most 30 days") int holdPickupDays,
        @Min(value = 0, message = "Cannot be negative") @Max(value = 1000000, message = "Too high") int lostItemFee,
        @Min(value = 0, message = "Cannot be negative") @Max(value = 30, message = "At most 30 days") int reminderDaysBefore,
        @NotNull(message = "Choose a default member type") Long defaultMemberTypeId
) {
}
