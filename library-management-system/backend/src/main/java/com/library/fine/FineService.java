package com.library.fine;

import com.library.audit.AuditAction;
import com.library.audit.AuditService;
import com.library.circulation.Loan;
import com.library.common.exception.BadRequestException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.member.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Charges on member accounts and the payments and waivers against them. */
@Service
public class FineService {

    private final FineRepository fineRepository;
    private final FineTransactionRepository transactionRepository;
    private final AuditService auditService;
    private final Clock clock;

    public FineService(FineRepository fineRepository, FineTransactionRepository transactionRepository,
                       AuditService auditService, Clock clock) {
        this.fineRepository = fineRepository;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ assessing

    /** Charges a late fine for a loan if it came back late. @return the amount charged (0 if on time) */
    @Transactional(propagation = Propagation.MANDATORY)
    public int assessLateReturn(Loan loan, long daysLate, int finePerDay, User actor) {
        int amount = (int) daysLate * finePerDay;
        if (amount <= 0) {
            return 0;
        }
        String title = loan.getCopy().getBook().getTitle();
        Fine fine = fineRepository.save(new Fine(loan.getMember(), loan, FineType.LATE_RETURN, amount,
                daysLate + " day" + (daysLate == 1 ? "" : "s") + " late: " + title, LocalDate.now(clock)));
        auditService.record(actor, AuditAction.FINE_ASSESSED, "Fine", fine.getId(),
                "₹" + amount + " late fine for " + loan.getMember().getName() + " on \"" + title + "\"");
        return amount;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int assessLostItem(Loan loan, int fee, User actor) {
        if (fee <= 0) {
            return 0;
        }
        String title = loan.getCopy().getBook().getTitle();
        Fine fine = fineRepository.save(new Fine(loan.getMember(), loan, FineType.LOST_ITEM, fee,
                "Lost copy " + loan.getCopy().getBarcode() + ": " + title, LocalDate.now(clock)));
        auditService.record(actor, AuditAction.FINE_ASSESSED, "Fine", fine.getId(),
                "₹" + fee + " lost item fee for " + loan.getMember().getName() + " on \"" + title + "\"");
        return fee;
    }

    // ------------------------------------------------------------------ settling

    @Transactional
    public FineResponse pay(Long fineId, PaymentRequest request, User staff) {
        Fine fine = find(fineId);
        fine.applyPayment(request.amount());
        transactionRepository.save(new FineTransaction(fine, TransactionKind.PAYMENT, request.amount(),
                blankToNull(request.note()), staff, LocalDateTime.now(clock)));
        auditService.record(staff, AuditAction.FINE_PAID, "Fine", fine.getId(),
                "₹" + request.amount() + " paid by " + fine.getMember().getName() + " (" + fine.getDescription() + ")");
        return view(fine);
    }

    @Transactional
    public FineResponse waive(Long fineId, WaiverRequest request, User staff) {
        Fine fine = find(fineId);
        fine.applyWaiver(request.amount());
        transactionRepository.save(new FineTransaction(fine, TransactionKind.WAIVER, request.amount(),
                request.reason().trim(), staff, LocalDateTime.now(clock)));
        auditService.record(staff, AuditAction.FINE_WAIVED, "Fine", fine.getId(),
                "₹" + request.amount() + " waived for " + fine.getMember().getName() + ": " + request.reason().trim());
        return view(fine);
    }

    // ------------------------------------------------------------------ queries

    /** @param status open (default) | settled | all */
    @Transactional(readOnly = true)
    public List<FineResponse> list(String status) {
        List<Fine> fines = switch (status == null ? "open" : status.toLowerCase()) {
            case "open" -> fineRepository.findOpen();
            case "settled" -> fineRepository.findAllByOrderByCreatedOnDescIdDesc().stream().filter(Fine::isSettled).toList();
            case "all" -> fineRepository.findAllByOrderByCreatedOnDescIdDesc();
            default -> throw new BadRequestException("Unknown status: " + status);
        };
        return withTransactions(fines);
    }

    @Transactional(readOnly = true)
    public List<FineResponse> forMember(Long memberId) {
        return withTransactions(fineRepository.findByMemberIdOrderByCreatedOnDescIdDesc(memberId));
    }

    @Transactional(readOnly = true)
    public long outstanding(Long memberId) {
        return fineRepository.outstandingForMember(memberId);
    }

    private List<FineResponse> withTransactions(List<Fine> fines) {
        if (fines.isEmpty()) {
            return List.of();
        }
        Map<Long, List<FineTransaction>> byFine = transactionRepository
                .findByFineIdInOrderByCreatedAtAsc(fines.stream().map(Fine::getId).toList()).stream()
                .collect(Collectors.groupingBy(t -> t.getFine().getId()));
        return fines.stream().map(f -> FineResponse.from(f, byFine.getOrDefault(f.getId(), List.of()))).toList();
    }

    private FineResponse view(Fine fine) {
        return withTransactions(List.of(fine)).get(0);
    }

    private Fine find(Long id) {
        return fineRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Fine", id));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
