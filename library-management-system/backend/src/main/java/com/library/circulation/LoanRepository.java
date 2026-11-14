package com.library.circulation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    // ----- per member -----

    @EntityGraph(attributePaths = {"copy", "copy.book", "member"})
    List<Loan> findByMemberIdOrderByBorrowDateDescIdDesc(Long memberId);

    @EntityGraph(attributePaths = {"copy", "copy.book", "member"})
    List<Loan> findByMemberIdAndStateOrderByDueDateAsc(Long memberId, LoanState state);

    long countByMemberIdAndState(Long memberId, LoanState state);

    long countByMemberIdAndStateAndDueDateBefore(Long memberId, LoanState state, LocalDate date);

    long countByMemberId(Long memberId);

    boolean existsByMemberIdAndCopyBookIdAndState(Long memberId, Long bookId, LoanState state);

    // ----- per copy / title -----

    @EntityGraph(attributePaths = {"copy", "copy.book", "member", "member.memberType"})
    Optional<Loan> findFirstByCopyIdAndState(Long copyId, LoanState state);

    boolean existsByCopyBookId(Long bookId);

    // ----- whole library -----

    @EntityGraph(attributePaths = {"copy", "copy.book", "member"})
    List<Loan> findAllByOrderByBorrowDateDescIdDesc();

    @EntityGraph(attributePaths = {"copy", "copy.book", "member"})
    List<Loan> findByStateOrderByDueDateAsc(LoanState state);

    @EntityGraph(attributePaths = {"copy", "copy.book", "member", "member.memberType"})
    List<Loan> findByStateAndDueDateBeforeOrderByDueDateAsc(LoanState state, LocalDate date);

    @EntityGraph(attributePaths = {"copy", "copy.book", "member"})
    List<Loan> findByStateInOrderByReturnDateDesc(List<LoanState> states);

    @EntityGraph(attributePaths = {"copy", "copy.book", "member"})
    List<Loan> findByStateAndDueDate(LoanState state, LocalDate dueDate);

    @EntityGraph(attributePaths = {"copy", "copy.book", "member"})
    List<Loan> findTop8ByOrderByIdDesc();

    long countByState(LoanState state);

    long countByStateAndDueDateBefore(LoanState state, LocalDate date);

    // ----- reports -----

    @EntityGraph(attributePaths = {"copy", "copy.book", "member"})
    List<Loan> findByBorrowDateBetweenOrderByBorrowDateAsc(LocalDate from, LocalDate to);

    long countByReturnDateBetweenAndState(LocalDate from, LocalDate to, LoanState state);

    @Query("SELECT l.returnDate, COUNT(l) FROM Loan l WHERE l.state = com.library.circulation.LoanState.RETURNED " +
            "AND l.returnDate BETWEEN :from AND :to GROUP BY l.returnDate")
    List<Object[]> countReturnsPerDay(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Rows of [bookId, title, author, count], most borrowed first. */
    @Query("SELECT b.id, b.title, b.author, COUNT(l) FROM Loan l JOIN l.copy c JOIN c.book b " +
            "WHERE l.borrowDate BETWEEN :from AND :to GROUP BY b.id, b.title, b.author ORDER BY COUNT(l) DESC")
    List<Object[]> topTitles(@Param("from") LocalDate from, @Param("to") LocalDate to,
                             org.springframework.data.domain.Pageable pageable);
}
