package com.library.circulation;

import com.library.common.security.StaffOnly;
import com.library.member.MemberDetail;
import com.library.member.MemberService;
import com.library.member.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The circulation desk: scan a card to see the member, scan copies to issue or check them in. */
@RestController
@RequestMapping("/api/desk")
@StaffOnly
public class DeskController {

    private final CirculationService circulationService;
    private final MemberService memberService;

    public DeskController(CirculationService circulationService, MemberService memberService) {
        this.circulationService = circulationService;
        this.memberService = memberService;
    }

    @GetMapping("/patrons/{cardNumber}")
    public MemberDetail patron(@PathVariable String cardNumber) {
        return memberService.detailByCard(cardNumber);
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse checkout(@Valid @RequestBody CheckoutRequest request, @AuthenticationPrincipal User librarian) {
        return circulationService.checkoutAtDesk(request, librarian);
    }

    @PostMapping("/checkin")
    public CheckInResult checkin(@Valid @RequestBody CheckinRequest request, @AuthenticationPrincipal User librarian) {
        return circulationService.checkIn(request.barcode(), librarian);
    }
}
