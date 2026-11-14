package com.library.catalog;

import com.library.audit.AuditAction;
import com.library.audit.AuditService;
import com.library.circulation.LoanRepository;
import com.library.circulation.LoanState;
import com.library.common.exception.BadRequestException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.common.web.PageResponse;
import com.library.hold.HoldRepository;
import com.library.hold.HoldService;
import com.library.hold.HoldStatus;
import com.library.member.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Titles and their physical copies. */
@Service
public class CatalogService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("title", "author", "publishedYear", "createdAt");

    private final BookRepository bookRepository;
    private final CopyRepository copyRepository;
    private final LoanRepository loanRepository;
    private final HoldRepository holdRepository;
    private final HoldService holdService;
    private final AuditService auditService;
    private final Clock clock;

    public CatalogService(BookRepository bookRepository, CopyRepository copyRepository, LoanRepository loanRepository,
                          HoldRepository holdRepository, HoldService holdService, AuditService auditService, Clock clock) {
        this.bookRepository = bookRepository;
        this.copyRepository = copyRepository;
        this.loanRepository = loanRepository;
        this.holdRepository = holdRepository;
        this.holdService = holdService;
        this.auditService = auditService;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ titles

    @Transactional(readOnly = true)
    public PageResponse<BookResponse> search(String query, String category, int page, int size, String sortBy, String direction) {
        String sortField = SORTABLE_FIELDS.contains(sortBy) ? sortBy : "title";
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(dir, sortField));
        Page<Book> books = bookRepository.search(blankToNull(query), blankToNull(category), pageable);
        Map<Long, Availability> stock = availability(books.getContent().stream().map(Book::getId).toList());
        return PageResponse.of(books, books.getContent().stream()
                .map(b -> BookResponse.from(b, stock.getOrDefault(b.getId(), Availability.NONE))).toList());
    }

    @Transactional(readOnly = true)
    public BookResponse get(Long id) {
        Book book = findBook(id);
        return BookResponse.from(book, availability(List.of(id)).getOrDefault(id, Availability.NONE));
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return bookRepository.findAllCategories();
    }

    @Transactional
    public BookResponse create(BookRequest request, User staff) {
        String isbn = request.isbn().trim();
        if (bookRepository.existsByIsbn(isbn)) {
            throw new BadRequestException("A book with ISBN " + isbn + " already exists");
        }
        Book book = new Book(request.title().trim(), request.author().trim(), isbn, blankToNull(request.category()),
                request.publishedYear(), blankToNull(request.description()));
        bookRepository.save(book);
        int copies = request.initialCopies() == null ? 1 : request.initialCopies();
        for (int i = 0; i < copies; i++) {
            newCopy(book, blankToNull(request.shelfLocation()));
        }
        auditService.record(staff, AuditAction.BOOK_CREATED, "Book", book.getId(),
                "Added \"" + book.getTitle() + "\" with " + copies + " cop" + (copies == 1 ? "y" : "ies"));
        return get(book.getId());
    }

    @Transactional
    public BookResponse update(Long id, BookRequest request, User staff) {
        Book book = findBook(id);
        String isbn = request.isbn().trim();
        if (bookRepository.existsByIsbnAndIdNot(isbn, id)) {
            throw new BadRequestException("Another book already uses ISBN " + isbn);
        }
        book.setTitle(request.title().trim());
        book.setAuthor(request.author().trim());
        book.setIsbn(isbn);
        book.setCategory(blankToNull(request.category()));
        book.setPublishedYear(request.publishedYear());
        book.setDescription(blankToNull(request.description()));
        auditService.record(staff, AuditAction.BOOK_UPDATED, "Book", book.getId(), "Edited \"" + book.getTitle() + "\"");
        return get(id);
    }

    /** Only titles that were never lent can be deleted; otherwise withdraw their copies so history stays intact. */
    @Transactional
    public void delete(Long id, User staff) {
        Book book = findBook(id);
        if (loanRepository.existsByCopyBookId(id)) {
            throw new BadRequestException("\"" + book.getTitle() + "\" has loan history, so it can't be deleted. "
                    + "Withdraw its copies instead.");
        }
        holdRepository.deleteByBookId(id);
        copyRepository.deleteAll(copyRepository.findByBookIdOrderByIdAsc(id));
        bookRepository.delete(book);
        auditService.record(staff, AuditAction.BOOK_DELETED, "Book", id, "Deleted \"" + book.getTitle() + "\"");
    }

    // ------------------------------------------------------------------ copies

    @Transactional(readOnly = true)
    public List<CopyResponse> copies(Long bookId) {
        findBook(bookId);
        return copyRepository.findByBookIdOrderByIdAsc(bookId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CopyResponse copyByBarcode(String barcode) {
        String code = barcode.trim().toUpperCase();
        return toResponse(copyRepository.findByBarcode(code)
                .orElseThrow(() -> new ResourceNotFoundException("No copy with barcode " + code)));
    }

    /** New copies go straight to the holds queue if anyone is waiting for the title. */
    @Transactional
    public List<CopyResponse> addCopies(Long bookId, AddCopiesRequest request, User staff) {
        Book book = findBook(bookId);
        List<Copy> added = new java.util.ArrayList<>();
        for (int i = 0; i < request.count(); i++) {
            Copy copy = newCopy(book, blankToNull(request.shelfLocation()));
            holdService.allocate(copy, staff);
            added.add(copy);
        }
        auditService.record(staff, AuditAction.COPY_ADDED, "Book", book.getId(),
                "Added " + request.count() + " cop" + (request.count() == 1 ? "y" : "ies") + " of \"" + book.getTitle() + "\": "
                        + String.join(", ", added.stream().map(Copy::getBarcode).toList()));
        return added.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CopyResponse updateCopy(Long copyId, CopyUpdateRequest request, User staff) {
        Copy copy = copyRepository.findById(copyId).orElseThrow(() -> ResourceNotFoundException.of("Copy", copyId));
        CopyStatus from = copy.getStatus();
        CopyStatus to = request.status();
        if (from != to) {
            if (!from.isManuallySettable()) {
                throw new BadRequestException("Copy " + copy.getBarcode() + " is " + label(from)
                        + ". Check it in at the desk before changing its status.");
            }
            if (!to.isManuallySettable()) {
                throw new BadRequestException("A copy becomes " + label(to) + " only through the desk");
            }
        }
        copy.setShelfLocation(blankToNull(request.shelfLocation()));
        copy.setNote(blankToNull(request.note()));
        if (from != to) {
            if (to == CopyStatus.AVAILABLE) {
                holdService.allocate(copy, staff); // back in circulation: offer it to the queue first
            } else {
                copy.setStatus(to);
            }
        }
        auditService.record(staff, AuditAction.COPY_UPDATED, "Copy", copy.getId(),
                "Copy " + copy.getBarcode() + " of \"" + copy.getBook().getTitle() + "\""
                        + (from != to ? ": " + label(from) + " → " + label(copy.getStatus()) : " updated"));
        return toResponse(copy);
    }

    // ------------------------------------------------------------------ helpers

    /** Availability for many titles in two grouped queries rather than one per title. */
    @Transactional(readOnly = true)
    public Map<Long, Availability> availability(Collection<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, int[]> counts = new HashMap<>(); // total, available, onLoan, onHoldShelf, waiting
        for (Object[] row : copyRepository.countByStatusForBooks(bookIds)) {
            CopyStatus status = (CopyStatus) row[1];
            if (!status.isCirculating()) {
                continue;
            }
            int[] c = counts.computeIfAbsent((Long) row[0], k -> new int[5]);
            int n = ((Long) row[2]).intValue();
            c[0] += n;
            switch (status) {
                case AVAILABLE -> c[1] += n;
                case ON_LOAN -> c[2] += n;
                case ON_HOLD_SHELF -> c[3] += n;
                default -> { }
            }
        }
        for (Object[] row : holdRepository.countWaitingForBooks(bookIds)) {
            counts.computeIfAbsent((Long) row[0], k -> new int[5])[4] = ((Long) row[1]).intValue();
        }
        Map<Long, Availability> result = new HashMap<>();
        counts.forEach((id, c) -> result.put(id, new Availability(c[0], c[1], c[2], c[3], c[4])));
        return result;
    }

    /** Barcodes are SW + the copy id, e.g. SW000042. A temporary value is used until the id exists. */
    private Copy newCopy(Book book, String shelfLocation) {
        Copy copy = copyRepository.save(new Copy(book, "TMP-" + UUID.randomUUID(), shelfLocation, LocalDate.now(clock)));
        copy.setBarcode(String.format("SW%06d", copy.getId()));
        return copy;
    }

    private CopyResponse toResponse(Copy c) {
        String borrower = null;
        LocalDate due = null;
        String heldFor = null;
        LocalDate expires = null;
        if (c.getStatus() == CopyStatus.ON_LOAN) {
            var loan = loanRepository.findFirstByCopyIdAndState(c.getId(), LoanState.OPEN).orElse(null);
            if (loan != null) {
                borrower = loan.getMember().getName();
                due = loan.getDueDate();
            }
        } else if (c.getStatus() == CopyStatus.ON_HOLD_SHELF) {
            var hold = holdRepository.findFirstByCopyIdAndStatus(c.getId(), HoldStatus.READY).orElse(null);
            if (hold != null) {
                heldFor = hold.getMember().getName();
                expires = hold.getExpiresOn();
            }
        }
        return new CopyResponse(c.getId(), c.getBook().getId(), c.getBook().getTitle(), c.getBarcode(), c.getShelfLocation(),
                c.getStatus(), c.getNote(), c.getAcquiredOn(), borrower, due, heldFor, expires);
    }

    private Book findBook(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Book", id));
    }

    private static String label(CopyStatus s) {
        return s.name().toLowerCase().replace('_', ' ');
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
