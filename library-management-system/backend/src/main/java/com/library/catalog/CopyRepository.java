package com.library.catalog;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CopyRepository extends JpaRepository<Copy, Long> {

    @EntityGraph(attributePaths = "book")
    Optional<Copy> findByBarcode(String barcode);

    List<Copy> findByBookIdOrderByIdAsc(Long bookId);

    Optional<Copy> findFirstByBookIdAndStatusOrderByIdAsc(Long bookId, CopyStatus status);

    long countByStatus(CopyStatus status);

    boolean existsByBookIdAndStatusIn(Long bookId, Collection<CopyStatus> statuses);

    /** Rows of [bookId, status, count] for the given titles, used to show availability in one query. */
    @Query("SELECT c.book.id, c.status, COUNT(c) FROM Copy c WHERE c.book.id IN :bookIds GROUP BY c.book.id, c.status")
    List<Object[]> countByStatusForBooks(@Param("bookIds") Collection<Long> bookIds);

    /** Rows of [status, count] across the whole collection. */
    @Query("SELECT c.status, COUNT(c) FROM Copy c GROUP BY c.status")
    List<Object[]> countAllByStatus();
}
