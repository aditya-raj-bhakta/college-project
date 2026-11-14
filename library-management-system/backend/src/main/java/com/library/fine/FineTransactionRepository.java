package com.library.fine;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface FineTransactionRepository extends JpaRepository<FineTransaction, Long> {

    @EntityGraph(attributePaths = {"staff"})
    List<FineTransaction> findByFineIdInOrderByCreatedAtAsc(Collection<Long> fineIds);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FineTransaction t " +
            "WHERE t.kind = :kind AND t.createdAt >= :from AND t.createdAt < :to")
    long sumByKindBetween(@Param("kind") TransactionKind kind,
                          @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FineTransaction t WHERE t.kind = com.library.fine.TransactionKind.PAYMENT")
    long totalCollected();
}
