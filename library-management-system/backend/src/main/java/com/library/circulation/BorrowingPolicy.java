package com.library.circulation;

import com.library.fine.FineRepository;
import com.library.member.MemberType;
import com.library.member.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The rules a member must meet to borrow, renew or reserve. Kept in one place so the
 * member's screen, the desk and the API all give the same answer.
 *
 *   - the account is active (not suspended) and has a member type
 *   - fewer open loans than the member type allows
 *   - no overdue books
 *   - unpaid fines no higher than the member type's block threshold
 */
@Component
public class BorrowingPolicy {

    private final LoanRepository loanRepository;
    private final FineRepository fineRepository;

    public BorrowingPolicy(LoanRepository loanRepository, FineRepository fineRepository) {
        this.loanRepository = loanRepository;
        this.fineRepository = fineRepository;
    }

    @Transactional(readOnly = true)
    public Eligibility evaluate(User member, LocalDate today) {
        MemberType type = member.getMemberType();
        List<String> blocks = new ArrayList<>();
        long open = loanRepository.countByMemberIdAndState(member.getId(), LoanState.OPEN);
        long owed = fineRepository.outstandingForMember(member.getId());

        if (member.isSuspended()) {
            blocks.add("Account suspended" + (member.getSuspendedReason() == null ? "" : ": " + member.getSuspendedReason()));
        }
        if (type == null) {
            blocks.add("No member type set");
            return new Eligibility(false, blocks, open, 0, owed, 0);
        }
        if (open >= type.getMaxLoans()) {
            blocks.add("Loan limit reached (" + open + " of " + type.getMaxLoans() + ")");
        }
        long overdue = loanRepository.countByMemberIdAndStateAndDueDateBefore(member.getId(), LoanState.OPEN, today);
        if (overdue > 0) {
            blocks.add(overdue + " overdue book" + (overdue == 1 ? "" : "s") + " to return first");
        }
        if (owed > type.getFineBlockThreshold()) {
            blocks.add("Unpaid fines of ₹" + owed + " are over the ₹" + type.getFineBlockThreshold() + " limit");
        }
        return new Eligibility(blocks.isEmpty(), List.copyOf(blocks), open, type.getMaxLoans(), owed, type.getFineBlockThreshold());
    }

    /** Renewing needs an account in good standing, but not a free loan slot (the book is already out). */
    @Transactional(readOnly = true)
    public List<String> renewalBlocks(User member) {
        List<String> blocks = new ArrayList<>();
        MemberType type = member.getMemberType();
        if (member.isSuspended()) {
            blocks.add("Account suspended");
        }
        if (type != null) {
            long owed = fineRepository.outstandingForMember(member.getId());
            if (owed > type.getFineBlockThreshold()) {
                blocks.add("Unpaid fines of ₹" + owed + " are over the ₹" + type.getFineBlockThreshold() + " limit");
            }
        }
        return blocks;
    }
}
