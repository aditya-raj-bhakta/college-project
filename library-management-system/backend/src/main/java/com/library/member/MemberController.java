package com.library.member;

import com.library.common.security.StaffOnly;
import com.library.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@StaffOnly
public class MemberController {

    private final MemberService memberService;
    private final MemberTypeRepository memberTypeRepository;

    public MemberController(MemberService memberService, MemberTypeRepository memberTypeRepository) {
        this.memberService = memberService;
        this.memberTypeRepository = memberTypeRepository;
    }

    /** Member types and their rules, read-only for librarians (admins edit them on /api/admin). */
    @GetMapping("/types")
    public java.util.List<MemberTypeResponse> types() {
        return memberTypeRepository.findAllByOrderByIdAsc().stream().map(MemberTypeResponse::from).toList();
    }

    @GetMapping
    public PageResponse<MemberSummary> search(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) AccountStatus status,
                                              @RequestParam(required = false) Long typeId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "50") int size) {
        return memberService.search(q, status, typeId, page, size);
    }

    @GetMapping("/{id}")
    public MemberDetail detail(@PathVariable Long id) {
        return memberService.detail(id);
    }

    @PutMapping("/{id}/type")
    public MemberDetail changeType(@PathVariable Long id, @Valid @RequestBody ChangeTypeRequest request,
                                   @AuthenticationPrincipal User staff) {
        return memberService.changeType(id, request, staff);
    }

    @PostMapping("/{id}/suspend")
    public MemberDetail suspend(@PathVariable Long id, @Valid @RequestBody SuspendRequest request,
                                @AuthenticationPrincipal User staff) {
        return memberService.suspend(id, request, staff);
    }

    @PostMapping("/{id}/reactivate")
    public MemberDetail reactivate(@PathVariable Long id, @AuthenticationPrincipal User staff) {
        return memberService.reactivate(id, staff);
    }
}
