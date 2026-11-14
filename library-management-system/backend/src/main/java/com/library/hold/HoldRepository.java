package com.library.hold;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HoldRepository extends JpaRepository<Hold, Long> {

    /** The front of the queue for a title. */
    @EntityGraph(attributePaths = {"member", "book"})
    Optional<Hold> findFirstByBookIdAndStatusOrderByPlacedAtAscIdAsc(Long bookId, HoldStatus status);

    @EntityGraph(attributePaths = {"member", "book", "copy"})
    Optional<Hold> findFirstByMemberIdAndBookIdAndStatus(Long memberId, Long bookId, HoldStatus status);

    @EntityGraph(attributePaths = {"member", "book", "copy"})
    Optional<Hold> findFirstByCopyIdAndStatus(Long copyId, HoldStatus status);

    boolean existsByMemberIdAndBookIdAndStatusIn(Long memberId, Long bookId, Collection<HoldStatus> statuses);

    long countByMemberIdAndStatusIn(Long memberId, Collection<HoldStatus> statuses);

    long countByBookIdAndStatus(Long bookId, HoldStatus status);

    long countByStatus(HoldStatus status);

    /** Position in the queue: WAITING holds placed before this one, plus one. */
    @Query("SELECT COUNT(h) + 1 FROM Hold h WHERE h.book.id = :bookId AND h.status = com.library.hold.HoldStatus.WAITING " +
            "AND (h.placedAt < :placedAt OR (h.placedAt = :placedAt AND h.id < :id))")
    long queuePosition(@Param("bookId") Long bookId, @Param("placedAt") LocalDateTime placedAt, @Param("id") Long id);

    @EntityGraph(attributePaths = {"member", "book", "copy"})
    List<Hold> findByMemberIdOrderByPlacedAtDesc(Long memberId);

    @EntityGraph(attributePaths = {"member", "book", "copy"})
    List<Hold> findByMemberIdAndStatusInOrderByPlacedAtAsc(Long memberId, Collection<HoldStatus> statuses);

    @EntityGraph(attributePaths = {"member", "book", "copy"})
    List<Hold> findByStatusInOrderByPlacedAtAsc(Collection<HoldStatus> statuses);

    @EntityGraph(attributePaths = {"member", "book", "copy"})
    List<Hold> findByBookIdAndStatusInOrderByPlacedAtAsc(Long bookId, Collection<HoldStatus> statuses);

    @EntityGraph(attributePaths = {"member", "book", "copy"})
    List<Hold> findByStatusAndExpiresOnBefore(HoldStatus status, LocalDate date);

    /** Rows of [bookId, count] of WAITING holds for the given titles. */
    @Query("SELECT h.book.id, COUNT(h) FROM Hold h WHERE h.status = com.library.hold.HoldStatus.WAITING " +
            "AND h.book.id IN :bookIds GROUP BY h.book.id")
    List<Object[]> countWaitingForBooks(@Param("bookIds") Collection<Long> bookIds);

    void deleteByBookId(Long bookId);
}
