package com.library.catalog;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CopyUpdateRequest(
        @NotNull(message = "Choose a status") CopyStatus status,
        @Size(max = 60) String shelfLocation,
        @Size(max = 255) String note
) {
}
