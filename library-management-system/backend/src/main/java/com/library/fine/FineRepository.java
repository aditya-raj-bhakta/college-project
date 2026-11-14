package com.library.fine;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FineRepository extends JpaRepository<Fine, Long> {

    @EntityGraph(attributePaths = {"member", "loan", "loan.copy", "loan.copy.book"})
    List<Fine> findByMemberIdOrderByCreatedOnDescIdDesc(Long memberId);

    @EntityGraph(attributePaths = {"member", "loan", "loan.copy", "loan.copy.book"})
    List<Fine> findAllByOrderByCreatedOnDescIdDesc();

    @EntityGraph(attributePaths = {"member", "loan", "loan.copy", "loan.copy.book"})
    @Query("SELECT f FROM Fine f WHERE f.amount - f.paid - f.waived > 0 ORDER BY f.createdOn ASC, f.id ASC")
    List<Fine> findOpen();

    @EntityGraph(attributePaths = {"member", "loan", "loan.copy", "loan.copy.book"})
    List<Fine> findByCreatedOnBetweenOrderByCreatedOnAsc(LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(f.amount - f.paid - f.waived), 0) FROM Fine f WHERE f.member.id = :memberId")
    long outstandingForMember(@Param("memberId") Long memberId);

    @Query("SELECT COALESCE(SUM(f.amount - f.paid - f.waived), 0) FROM Fine f")
    long outstandingTotal();

    @Query("SELECT COALESCE(SUM(f.paid), 0) FROM Fine f WHERE f.member.id = :memberId")
    long paidByMember(@Param("memberId") Long memberId);

    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM Fine f WHERE f.createdOn BETWEEN :from AND :to")
    long assessedBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
