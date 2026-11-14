package com.library.catalog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AddCopiesRequest(
        @Min(value = 1, message = "Add at least one copy") @Max(value = 200, message = "At most 200 at once") int count,
        @Size(max = 60) String shelfLocation
) {
}
