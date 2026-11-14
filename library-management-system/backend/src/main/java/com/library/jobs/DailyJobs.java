package com.library.jobs;

import com.library.audit.AuditAction;
import com.library.audit.AuditService;
import com.library.circulation.Loan;
import com.library.circulation.LoanRepository;
import com.library.circulation.LoanState;
import com.library.hold.HoldService;
import com.library.member.User;
import com.library.notification.NotificationService;
import com.library.settings.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Runs once a day (app.jobs.daily-cron), and on demand from the admin Rules page:
 *   1. holds not collected in time expire, and their copies go to the next member
 *   2. members get a reminder a few days before a book is due
 *   3. members get one notice when a book becomes overdue
 * Each reminder is sent at most once per loan, so running it twice is harmless.
 */
@Component
public class DailyJobs {

    private static final Logger log = LoggerFactory.getLogger(DailyJobs.class);

    private final HoldService holdService;
    private final LoanRepository loanRepository;
    private final NotificationService notificationService;
    private final SettingsService settingsService;
    private final AuditService auditService;
    private final Clock clock;

    public DailyJobs(HoldService holdService, LoanRepository loanRepository, NotificationService notificationService,
                     SettingsService settingsService, AuditService auditService, Clock clock) {
        this.holdService = holdService;
        this.loanRepository = loanRepository;
        this.notificationService = notificationService;
        this.settingsService = settingsService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.jobs.daily-cron}")
    public void scheduledRun() {
        JobResult result = run(null);
        log.info("Daily jobs: {} holds expired, {} due-soon reminders, {} overdue notices",
                result.holdsExpired(), result.dueSoonReminders(), result.overdueNotices());
    }

    @Transactional
    public JobResult run(User triggeredBy) {
        LocalDate today = LocalDate.now(clock);
        int expired = holdService.expireUncollected();

        int dueSoon = 0;
        LocalDate remindFor = today.plusDays(settingsService.current().getReminderDaysBefore());
        for (Loan loan : loanRepository.findByStateAndDueDate(LoanState.OPEN, remindFor)) {
            if (notificationService.dueSoon(loan)) {
                dueSoon++;
            }
        }

        int overdue = 0;
        for (Loan loan : loanRepository.findByStateAndDueDateBeforeOrderByDueDateAsc(LoanState.OPEN, today)) {
            int rate = loan.getMember().getMemberType() == null ? 0 : loan.getMember().getMemberType().getFinePerDay();
            if (notificationService.overdue(loan, rate)) {
                overdue++;
            }
        }

        auditService.record(triggeredBy, AuditAction.JOB_RUN, "Job", null,
                "Daily jobs: " + expired + " holds expired, " + dueSoon + " due-soon reminders, " + overdue + " overdue notices");
        return new JobResult(LocalDateTime.now(clock), expired, dueSoon, overdue);
    }
}
