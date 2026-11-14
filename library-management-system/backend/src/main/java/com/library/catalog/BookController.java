package com.library.catalog;

import com.library.common.security.StaffOnly;
import com.library.common.web.PageResponse;
import com.library.hold.HoldResponse;
import com.library.hold.HoldService;
import com.library.member.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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

/** Anyone logged in can browse titles; staff manage titles, copies and see the queue. */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final CatalogService catalogService;
    private final HoldService holdService;

    public BookController(CatalogService catalogService, HoldService holdService) {
        this.catalogService = catalogService;
        this.holdService = holdService;
    }

    /** Example: GET /api/books?q=java&category=Programming&page=0&size=12&sort=title&dir=asc */
    @GetMapping
    public PageResponse<BookResponse> search(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "12") int size,
                                             @RequestParam(defaultValue = "title") String sort,
                                             @RequestParam(defaultValue = "asc") String dir) {
        return catalogService.search(q, category, page, size, sort, dir);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return catalogService.categories();
    }

    @GetMapping("/{id}")
    public BookResponse get(@PathVariable Long id) {
        return catalogService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @StaffOnly
    public BookResponse create(@Valid @RequestBody BookRequest request, @AuthenticationPrincipal User staff) {
        return catalogService.create(request, staff);
    }

    @PutMapping("/{id}")
    @StaffOnly
    public BookResponse update(@PathVariable Long id, @Valid @RequestBody BookRequest request,
                               @AuthenticationPrincipal User staff) {
        return catalogService.update(id, request, staff);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @StaffOnly
    public void delete(@PathVariable Long id, @AuthenticationPrincipal User staff) {
        catalogService.delete(id, staff);
    }

    @GetMapping("/{id}/copies")
    @StaffOnly
    public List<CopyResponse> copies(@PathVariable Long id) {
        return catalogService.copies(id);
    }

    @PostMapping("/{id}/copies")
    @ResponseStatus(HttpStatus.CREATED)
    @StaffOnly
    public List<CopyResponse> addCopies(@PathVariable Long id, @Valid @RequestBody AddCopiesRequest request,
                                        @AuthenticationPrincipal User staff) {
        return catalogService.addCopies(id, request, staff);
    }

    @GetMapping("/{id}/holds")
    @StaffOnly
    public List<HoldResponse> queue(@PathVariable Long id) {
        return holdService.queueForBook(id);
    }
}
