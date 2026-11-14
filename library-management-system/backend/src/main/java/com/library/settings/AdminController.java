package com.library.settings;

import com.library.audit.AuditAction;
import com.library.audit.AuditEventResponse;
import com.library.audit.AuditQueryService;
import com.library.common.security.AdminOnly;
import com.library.common.web.PageResponse;
import com.library.jobs.DailyJobs;
import com.library.jobs.JobResult;
import com.library.member.MemberService;
import com.library.member.MemberTypeResponse;
import com.library.member.StaffRequest;
import com.library.member.User;
import com.library.member.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin only: library rules, staff accounts, the audit log and the daily job. */
@RestController
@RequestMapping("/api/admin")
@AdminOnly
public class AdminController {

    private final SettingsService settingsService;
    private final MemberService memberService;
    private final AuditQueryService auditQueryService;
    private final DailyJobs dailyJobs;

    public AdminController(SettingsService settingsService, MemberService memberService,
                           AuditQueryService auditQueryService, DailyJobs dailyJobs) {
        this.settingsService = settingsService;
        this.memberService = memberService;
        this.auditQueryService = auditQueryService;
        this.dailyJobs = dailyJobs;
    }

    @GetMapping("/settings")
    public SettingsResponse settings() {
        return settingsService.view();
    }

    @PutMapping("/settings")
    public SettingsResponse updateSettings(@Valid @RequestBody SettingsRequest request, @AuthenticationPrincipal User admin) {
        return settingsService.update(request, admin);
    }

    @PutMapping("/member-types/{id}")
    public MemberTypeResponse updateMemberType(@PathVariable Long id, @Valid @RequestBody MemberTypeRequest request,
                                               @AuthenticationPrincipal User admin) {
        return settingsService.updateMemberType(id, request, admin);
    }

    @GetMapping("/staff")
    public List<UserResponse> staff() {
        return memberService.staff();
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createStaff(@Valid @RequestBody StaffRequest request, @AuthenticationPrincipal User admin) {
        return memberService.createStaff(request, admin);
    }

    @GetMapping("/audit")
    public PageResponse<AuditEventResponse> audit(@RequestParam(required = false) AuditAction action,
                                                  @RequestParam(required = false) String q,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "50") int size) {
        return auditQueryService.search(action, q, page, size);
    }

    @PostMapping("/jobs/daily")
    public JobResult runDailyJobs(@AuthenticationPrincipal User admin) {
        return dailyJobs.run(admin);
    }
}
