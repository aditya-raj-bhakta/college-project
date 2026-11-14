package com.library.circulation;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(
        @NotBlank(message = "Scan or type the library card number") String cardNumber,
        @NotBlank(message = "Scan or type the copy barcode") String barcode
) {
}
