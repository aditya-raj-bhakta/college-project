package com.library.circulation;

import jakarta.validation.constraints.NotNull;

public record SelfBorrowRequest(@NotNull(message = "Choose a book") Long bookId) {
}
