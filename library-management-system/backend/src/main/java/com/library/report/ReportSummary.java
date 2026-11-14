package com.library.report;

import java.time.LocalDate;
import java.util.List;

/**
 * Circulation for a date range.
 *
 * @param daily one row per day in the range, including days with no activity
 */
public record ReportSummary(
        LocalDate from,
        LocalDate to,
        long checkouts,
        long returns,
        long newMembers,
        long finesAssessed,
        long finesCollected,
        long finesWaived,
        long overdueNow,
        List<Day> daily,
        List<TopTitle> topTitles
) {
    public record Day(LocalDate date, long checkouts, long returns) {
    }

    public record TopTitle(Long bookId, String title, String author, long checkouts) {
    }
}
