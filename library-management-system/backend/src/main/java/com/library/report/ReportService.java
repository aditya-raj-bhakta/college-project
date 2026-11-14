package com.library.report;

import com.library.circulation.Loan;
import com.library.circulation.LoanRepository;
import com.library.circulation.LoanState;
import com.library.common.exception.BadRequestException;
import com.library.fine.Fine;
import com.library.fine.FineRepository;
import com.library.fine.FineTransactionRepository;
import com.library.fine.TransactionKind;
import com.library.member.Role;
import com.library.member.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private static final int MAX_RANGE_DAYS = 366;

    private final LoanRepository loanRepository;
    private final FineRepository fineRepository;
    private final FineTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public ReportService(LoanRepository loanRepository, FineRepository fineRepository,
                         FineTransactionRepository transactionRepository, UserRepository userRepository, Clock clock) {
        this.loanRepository = loanRepository;
        this.fineRepository = fineRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ReportSummary summary(LocalDate from, LocalDate to) {
        checkRange(from, to);
        LocalDate today = LocalDate.now(clock);
        List<Loan> borrowed = loanRepository.findByBorrowDateBetweenOrderByBorrowDateAsc(from, to);

        Map<LocalDate, long[]> perDay = new HashMap<>();
        for (Loan l : borrowed) {
            perDay.computeIfAbsent(l.getBorrowDate(), d -> new long[2])[0]++;
        }
        for (Object[] row : loanRepository.countReturnsPerDay(from, to)) {
            perDay.computeIfAbsent((LocalDate) row[0], d -> new long[2])[1] = (Long) row[1];
        }
        List<ReportSummary.Day> daily = new ArrayList<>();
        long returns = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            long[] c = perDay.getOrDefault(d, new long[2]);
            returns += c[1];
            daily.add(new ReportSummary.Day(d, c[0], c[1]));
        }

        List<ReportSummary.TopTitle> top = loanRepository.topTitles(from, to, PageRequest.of(0, 8)).stream()
                .map(r -> new ReportSummary.TopTitle((Long) r[0], (String) r[1], (String) r[2], (Long) r[3]))
                .toList();

        return new ReportSummary(from, to, borrowed.size(), returns,
                userRepository.countByRoleAndCreatedAtBetween(Role.MEMBER, from.atStartOfDay(), to.plusDays(1).atStartOfDay()),
                fineRepository.assessedBetween(from, to),
                transactionRepository.sumByKindBetween(TransactionKind.PAYMENT, from.atStartOfDay(), to.plusDays(1).atStartOfDay()),
                transactionRepository.sumByKindBetween(TransactionKind.WAIVER, from.atStartOfDay(), to.plusDays(1).atStartOfDay()),
                loanRepository.countByStateAndDueDateBefore(LoanState.OPEN, today),
                daily, top);
    }

    @Transactional(readOnly = true)
    public String loansCsv(LocalDate from, LocalDate to) {
        checkRange(from, to);
        LocalDate today = LocalDate.now(clock);
        Csv csv = new Csv("Loan", "Title", "Barcode", "Member", "Card", "Borrowed", "Due", "Returned", "Status", "Renewals", "Channel");
        for (Loan l : loanRepository.findByBorrowDateBetweenOrderByBorrowDateAsc(from, to)) {
            csv.row(l.getId(), l.getCopy().getBook().getTitle(), l.getCopy().getBarcode(), l.getMember().getName(),
                    l.getMember().getCardNumber(), l.getBorrowDate(), l.getDueDate(), l.getReturnDate(),
                    l.getStatus(today), l.getRenewalCount(), l.getChannel());
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String overdueCsv() {
        LocalDate today = LocalDate.now(clock);
        Csv csv = new Csv("Loan", "Title", "Barcode", "Member", "Card", "Email", "Due", "Days overdue", "Fine so far");
        for (Loan l : loanRepository.findByStateAndDueDateBeforeOrderByDueDateAsc(LoanState.OPEN, today)) {
            long days = ChronoUnit.DAYS.between(l.getDueDate(), today);
            int rate = l.getMember().getMemberType() == null ? 0 : l.getMember().getMemberType().getFinePerDay();
            csv.row(l.getId(), l.getCopy().getBook().getTitle(), l.getCopy().getBarcode(), l.getMember().getName(),
                    l.getMember().getCardNumber(), l.getMember().getEmail(), l.getDueDate(), days, days * rate);
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String finesCsv(LocalDate from, LocalDate to) {
        checkRange(from, to);
        Csv csv = new Csv("Fine", "Date", "Member", "Card", "Type", "Description", "Amount", "Paid", "Waived", "Balance");
        for (Fine f : fineRepository.findByCreatedOnBetweenOrderByCreatedOnAsc(from, to)) {
            csv.row(f.getId(), f.getCreatedOn(), f.getMember().getName(), f.getMember().getCardNumber(), f.getType(),
                    f.getDescription(), f.getAmount(), f.getPaid(), f.getWaived(), f.getBalance());
        }
        return csv.toString();
    }

    private static void checkRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("The start date is after the end date");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new BadRequestException("Choose a range of at most a year");
        }
    }

    /** Minimal RFC 4180 CSV: values with commas, quotes or newlines are quoted. */
    static final class Csv {
        private final StringBuilder out = new StringBuilder();

        Csv(String... header) {
            row((Object[]) header);
        }

        void row(Object... values) {
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    out.append(',');
                }
                String v = values[i] == null ? "" : values[i].toString();
                if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
                    v = "\"" + v.replace("\"", "\"\"") + "\"";
                }
                out.append(v);
            }
            out.append("\r\n");
        }

        @Override
        public String toString() {
            return out.toString();
        }
    }
}
