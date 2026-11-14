package com.library.catalog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param initialCopies only used when adding a new title: how many copies to create
 * @param shelfLocation only used when adding a new title: where those copies live
 */
public record BookRequest(
        @NotBlank(message = "Title is required") @Size(max = 200) String title,
        @NotBlank(message = "Author is required") @Size(max = 150) String author,
        @NotBlank(message = "ISBN is required")
        @Pattern(regexp = "^[0-9Xx-]{10,17}$", message = "ISBN must be 10-17 characters: digits, hyphens or X")
        String isbn,
        @Size(max = 60) String category,
        @Min(value = 1000, message = "Enter a valid year") @Max(value = 2100, message = "Enter a valid year") Integer publishedYear,
        @Size(max = 1000) String description,
        @Min(value = 0, message = "Cannot be negative") @Max(value = 200, message = "At most 200 copies at once") Integer initialCopies,
        @Size(max = 60) String shelfLocation
) {
}
