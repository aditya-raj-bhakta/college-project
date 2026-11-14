package com.library.jobs;

import java.time.LocalDateTime;

public record JobResult(LocalDateTime ranAt, int holdsExpired, int dueSoonReminders, int overdueNotices) {
}
