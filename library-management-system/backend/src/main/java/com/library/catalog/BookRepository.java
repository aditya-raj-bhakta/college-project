package com.library.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    /** Search by title, author or ISBN, optionally filtered by category. Null parameters are ignored. */
    @Query("""
            SELECT b FROM Book b
            WHERE (:q IS NULL
                   OR LOWER(b.title)  LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(b.isbn)   LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:category IS NULL OR b.category = :category)
            """)
    Page<Book> search(@Param("q") String q, @Param("category") String category, Pageable pageable);

    @Query("SELECT DISTINCT b.category FROM Book b WHERE b.category IS NOT NULL ORDER BY b.category")
    List<String> findAllCategories();
}
