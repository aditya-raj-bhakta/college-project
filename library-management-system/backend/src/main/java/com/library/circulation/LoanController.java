package com.library.circulation;

import com.library.common.security.StaffOnly;
import com.library.member.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** All loans, for staff. Members use /api/me/loans. */
@RestController
@RequestMapping("/api/loans")
@StaffOnly
public class LoanController {

    private final CirculationService circulationService;

    public LoanController(CirculationService circulationService) {
        this.circulationService = circulationService;
    }

    /** status = all | active | overdue | returned | lost */
    @GetMapping
    public List<LoanResponse> list(@RequestParam(defaultValue = "all") String status) {
        return circulationService.list(status);
    }

    @PostMapping("/{id}/checkin")
    public CheckInResult checkIn(@PathVariable Long id, @AuthenticationPrincipal User librarian) {
        return circulationService.checkInLoan(id, librarian);
    }

    @PostMapping("/{id}/renew")
    public LoanResponse renew(@PathVariable Long id, @AuthenticationPrincipal User librarian) {
        return circulationService.renew(id, librarian);
    }

    @PostMapping("/{id}/lost")
    public LoanResponse markLost(@PathVariable Long id, @AuthenticationPrincipal User librarian) {
        return circulationService.markLost(id, librarian);
    }
}
