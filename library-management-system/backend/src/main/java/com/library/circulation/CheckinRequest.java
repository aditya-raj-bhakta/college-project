package com.library.circulation;

import jakarta.validation.constraints.NotBlank;

public record CheckinRequest(@NotBlank(message = "Scan or type the copy barcode") String barcode) {
}
