package com.library.selfservice;

import com.library.circulation.CirculationService;
import com.library.circulation.LoanResponse;
import com.library.circulation.SelfBorrowRequest;
import com.library.dashboard.DashboardService;
import com.library.dashboard.MemberDashboard;
import com.library.fine.FineResponse;
import com.library.fine.FineService;
import com.library.hold.HoldResponse;
import com.library.hold.HoldService;
import com.library.member.User;
import com.library.member.UserRepository;
import com.library.member.UserResponse;
import com.library.notification.NotificationResponse;
import com.library.notification.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Self-service for the logged-in member: their account, loans, holds, fines and messages.
 * Everything here is scoped to the caller; nobody can reach another member's data through it.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserRepository userRepository;
    private final DashboardService dashboardService;
    private final CirculationService circulationService;
    private final HoldService holdService;
    private final FineService fineService;
    private final NotificationService notificationService;

    public MeController(UserRepository userRepository, DashboardService dashboardService,
                        CirculationService circulationService, HoldService holdService, FineService fineService,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.dashboardService = dashboardService;
        this.circulationService = circulationService;
        this.holdService = holdService;
        this.fineService = fineService;
        this.notificationService = notificationService;
    }

    /** The logged-in user. The frontend calls this on page load to check the saved token. */
    @GetMapping
    public UserResponse me(@AuthenticationPrincipal User user) {
        return UserResponse.from(userRepository.findByEmail(user.getEmail()).orElse(user));
    }

    @GetMapping("/dashboard")
    public MemberDashboard dashboard(@AuthenticationPrincipal User user) {
        return dashboardService.member(user);
    }

    @GetMapping("/loans")
    public List<LoanResponse> loans(@AuthenticationPrincipal User user) {
        return circulationService.loansForMember(user.getId());
    }

    /** Borrow online: the system picks a free copy, or the copy held for this member. */
    @PostMapping("/loans")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse borrow(@Valid @RequestBody SelfBorrowRequest request, @AuthenticationPrincipal User user) {
        return circulationService.selfBorrow(user, request.bookId());
    }

    @PostMapping("/loans/{id}/renew")
    public LoanResponse renew(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return circulationService.renew(id, user);
    }

    @GetMapping("/holds")
    public List<HoldResponse> holds(@AuthenticationPrincipal User user) {
        return holdService.forMember(user.getId());
    }

    @PostMapping("/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public HoldResponse placeHold(@Valid @RequestBody HoldRequest request, @AuthenticationPrincipal User user) {
        return holdService.place(user, request.bookId());
    }

    @DeleteMapping("/holds/{id}")
    public HoldResponse cancelHold(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return holdService.cancel(id, user);
    }

    @GetMapping("/fines")
    public List<FineResponse> fines(@AuthenticationPrincipal User user) {
        return fineService.forMember(user.getId());
    }

    @GetMapping("/notifications")
    public List<NotificationResponse> notifications(@AuthenticationPrincipal User user) {
        return notificationService.recentFor(user.getId());
    }

    public record HoldRequest(@NotNull(message = "Choose a book") Long bookId) {
    }
}
