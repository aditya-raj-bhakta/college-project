package com.library.catalog;

import java.time.LocalDateTime;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        String category,
        Integer publishedYear,
        String description,
        LocalDateTime createdAt,
        Availability availability
) {
    static BookResponse from(Book b, Availability availability) {
        return new BookResponse(b.getId(), b.getTitle(), b.getAuthor(), b.getIsbn(), b.getCategory(),
                b.getPublishedYear(), b.getDescription(), b.getCreatedAt(), availability);
    }
}
