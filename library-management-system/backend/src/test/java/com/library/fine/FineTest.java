package com.library.fine;

import com.library.common.exception.BadRequestException;
import com.library.member.Role;
import com.library.member.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The fine ledger rules, without a database. */
class FineTest {

    private final User member = new User("M", "m@test.com", "x", Role.MEMBER);

    @Test
    void paymentsAndWaiversReduceTheBalanceUntilSettled() {
        Fine fine = new Fine(member, null, FineType.LATE_RETURN, 100, "late", LocalDate.of(2026, 1, 1));
        fine.applyPayment(30);
        fine.applyWaiver(20);
        assertThat(fine.getBalance()).isEqualTo(50);
        assertThat(fine.isSettled()).isFalse();
        fine.applyPayment(50);
        assertThat(fine.isSettled()).isTrue();
    }

    @Test
    void cannotPayMoreThanIsOwedOrNothingAtAll() {
        Fine fine = new Fine(member, null, FineType.LOST_ITEM, 500, "lost", LocalDate.of(2026, 1, 1));
        assertThatThrownBy(() -> fine.applyPayment(501)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> fine.applyWaiver(0)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void aFineMustBePositive() {
        assertThatThrownBy(() -> new Fine(member, null, FineType.LATE_RETURN, 0, "none", LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
