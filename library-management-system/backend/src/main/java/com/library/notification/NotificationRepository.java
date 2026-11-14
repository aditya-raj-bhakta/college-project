package com.library.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop20ByMemberIdOrderBySentAtDesc(Long memberId);

    boolean existsByLoanIdAndKind(Long loanId, NotificationKind kind);
}
