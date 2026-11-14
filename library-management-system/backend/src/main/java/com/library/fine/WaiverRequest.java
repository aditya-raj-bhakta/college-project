package com.library.fine;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WaiverRequest(
        @Min(value = 1, message = "Enter an amount greater than zero") int amount,
        @NotBlank(message = "Give a reason for the waiver") @Size(max = 255) String reason
) {
}
