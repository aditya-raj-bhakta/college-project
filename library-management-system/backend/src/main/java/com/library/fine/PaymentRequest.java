package com.library.fine;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record PaymentRequest(
        @Min(value = 1, message = "Enter an amount greater than zero") int amount,
        @Size(max = 255) String note
) {
}
