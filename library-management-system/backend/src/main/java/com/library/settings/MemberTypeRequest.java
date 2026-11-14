package com.library.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberTypeRequest(
        @NotBlank(message = "Name is required") @Size(max = 60) String name,
        @Min(value = 1, message = "At least 1") @Max(value = 100, message = "At most 100") int maxLoans,
        @Min(value = 1, message = "At least 1 day") @Max(value = 365, message = "At most 365 days") int loanDays,
        @Min(value = 0, message = "Cannot be negative") @Max(value = 20, message = "At most 20") int maxRenewals,
        @Min(value = 0, message = "Cannot be negative") @Max(value = 10000, message = "Too high") int finePerDay,
        @Min(value = 0, message = "Cannot be negative") @Max(value = 1000000, message = "Too high") int fineBlockThreshold,
        @Min(value = 0, message = "Cannot be negative") @Max(value = 100, message = "At most 100") int maxHolds
) {
}
