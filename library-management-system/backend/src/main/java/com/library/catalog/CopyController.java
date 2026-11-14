package com.library.catalog;

import com.library.common.security.StaffOnly;
import com.library.member.User;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/copies")
@StaffOnly
public class CopyController {

    private final CatalogService catalogService;

    public CopyController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/barcode/{barcode}")
    public CopyResponse byBarcode(@PathVariable String barcode) {
        return catalogService.copyByBarcode(barcode);
    }

    @PatchMapping("/{id}")
    public CopyResponse update(@PathVariable Long id, @Valid @RequestBody CopyUpdateRequest request,
                               @AuthenticationPrincipal User staff) {
        return catalogService.updateCopy(id, request, staff);
    }
}
