package com.library.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Dates as people read them in audit entries and messages, e.g. "17 Nov 2026". */
public final class Dates {

    private static final DateTimeFormatter HUMAN = DateTimeFormatter.ofPattern("d MMM yyyy");

    private Dates() {
    }

    public static String human(LocalDate date) {
        return date == null ? "" : HUMAN.format(date);
    }
}
