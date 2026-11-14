package com.library.hold;

import com.library.common.security.StaffOnly;
import com.library.member.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The holds queue across the library, for staff. Members use /api/me/holds. */
@RestController
@RequestMapping("/api/holds")
@StaffOnly
public class HoldController {

    private final HoldService holdService;

    public HoldController(HoldService holdService) {
        this.holdService = holdService;
    }

    /** status = active | waiting | ready */
    @GetMapping
    public List<HoldResponse> list(@RequestParam(defaultValue = "active") String status) {
        return holdService.list(status);
    }

    @DeleteMapping("/{id}")
    public HoldResponse cancel(@PathVariable Long id, @AuthenticationPrincipal User staff) {
        return holdService.cancel(id, staff);
    }
}
