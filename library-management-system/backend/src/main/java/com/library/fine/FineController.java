package com.library.fine;

import com.library.common.security.StaffOnly;
import com.library.member.User;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fines")
@StaffOnly
public class FineController {

    private final FineService fineService;

    public FineController(FineService fineService) {
        this.fineService = fineService;
    }

    /** status = open | settled | all */
    @GetMapping
    public List<FineResponse> list(@RequestParam(defaultValue = "open") String status) {
        return fineService.list(status);
    }

    @PostMapping("/{id}/payments")
    public FineResponse pay(@PathVariable Long id, @Valid @RequestBody PaymentRequest request,
                            @AuthenticationPrincipal User staff) {
        return fineService.pay(id, request, staff);
    }

    @PostMapping("/{id}/waivers")
    public FineResponse waive(@PathVariable Long id, @Valid @RequestBody WaiverRequest request,
                              @AuthenticationPrincipal User staff) {
        return fineService.waive(id, request, staff);
    }
}
